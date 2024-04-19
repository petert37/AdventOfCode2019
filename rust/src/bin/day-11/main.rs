use std::{collections::HashMap, fs};

use intcode::intcode_computer::IntcodeComputer;

#[tokio::main]
async fn main() {
    let part_1_result = part_1(&fs::read_to_string("src/bin/day-11/input.txt").unwrap()).await;
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-11/input.txt").unwrap()).await;
    println!("Part 2 result");
    println!("{}", part_2_result);
}

async fn part_1(input: &str) -> i64 {
    let result = run_robot(input, 0).await;
    result.len() as i64
}

async fn part_2(input: &str) -> String {
    let result = run_robot(input, 1).await;
    map_to_string(&result)
}

async fn run_robot(input: &str, start_color: i64) -> HashMap<(i64, i64), i64> {
    let mut map: HashMap<(i64, i64), i64> = HashMap::new();
    let mut position = (0, 0);
    let mut direction = Direction::Up;
    let (sender, computer_input) = tokio::sync::mpsc::channel(1);
    let (computer_output, mut receiver) = tokio::sync::mpsc::channel(1);
    let mut computer = IntcodeComputer::new_with_io(input, computer_input, computer_output);
    tokio::spawn(async move { computer.run().await });
    sender.send(start_color).await.unwrap();
    loop {
        let color = match receiver.recv().await {
            Some(color) => color,
            None => break,
        };
        let turn = match receiver.recv().await {
            Some(turn) => turn,
            None => break,
        };
        map.insert(position, color);
        direction = direction.turn(turn);
        position = direction.step(position);
        let _ = sender.send(*map.get(&position).unwrap_or(&0)).await;
    }
    map
}

enum Direction {
    Up,
    Down,
    Left,
    Right,
}

impl Direction {
    fn turn(&self, turn: i64) -> Self {
        match turn {
            0 => match self {
                Direction::Up => Direction::Left,
                Direction::Down => Direction::Right,
                Direction::Left => Direction::Down,
                Direction::Right => Direction::Up,
            },
            1 => match self {
                Direction::Up => Direction::Right,
                Direction::Down => Direction::Left,
                Direction::Left => Direction::Up,
                Direction::Right => Direction::Down,
            },
            _ => panic!("Invalid turn value"),
        }
    }

    fn step(&self, position: (i64, i64)) -> (i64, i64) {
        match self {
            Direction::Up => (position.0, position.1 - 1),
            Direction::Down => (position.0, position.1 + 1),
            Direction::Left => (position.0 - 1, position.1),
            Direction::Right => (position.0 + 1, position.1),
        }
    }
}

fn map_to_string(map: &HashMap<(i64, i64), i64>) -> String {
    let mut min_x = i64::MAX;
    let mut max_x = i64::MIN;
    let mut min_y = i64::MAX;
    let mut max_y = i64::MIN;
    for (x, y) in map.keys() {
        if *x < min_x {
            min_x = *x;
        }
        if *x > max_x {
            max_x = *x;
        }
        if *y < min_y {
            min_y = *y;
        }
        if *y > max_y {
            max_y = *y;
        }
    }
    let mut result = String::new();
    for y in min_y..=max_y {
        for x in min_x..=max_x {
            let color = *map.get(&(x, y)).unwrap_or(&0);
            result.push_str(if color == 0 { "." } else { "#" });
        }
        result.push('\n');
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_part_1_actual_input() {
        let result = part_1(&fs::read_to_string("src/bin/day-11/input.txt").unwrap()).await;
        assert_eq!(result, 2064);
    }

    #[tokio::test]
    async fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-11/input.txt").unwrap()).await;
        let expexted = &r#"
.#....###..####.#..#.#.....##..#..#.###....
.#....#..#....#.#.#..#....#..#.#..#.#..#...
.#....#..#...#..##...#....#....####.#..#...
.#....###...#...#.#..#....#.##.#..#.###....
.#....#....#....#.#..#....#..#.#..#.#.#....
.####.#....####.#..#.####..###.#..#.#..#...
"#[1..];
        assert_eq!(result, expexted.to_string());
    }
}
