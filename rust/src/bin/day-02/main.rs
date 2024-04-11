use std::fs;

use intcode::intcode_computer::IntcodeComputer;

fn main() {
    let part_1_result = part_1(
        &fs::read_to_string("src/bin/day-02/input.txt").unwrap(),
        true,
    );
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-02/input.txt").unwrap());
    println!("Part 2 result: {}", part_2_result);
}

fn part_1(input: &str, modify: bool) -> i64 {
    let mut computer = IntcodeComputer::new(input);
    if modify {
        computer.set_memory(1, 12);
        computer.set_memory(2, 2);
    }
    computer.run();
    computer.get_memory(0)
}

fn part_2(input: &str) -> i64 {
    let computer = IntcodeComputer::new(input);
    for noun in 0..100 {
        for verb in 0..100 {
            let mut computer = computer.clone();
            computer.set_memory(1, noun);
            computer.set_memory(2, verb);
            computer.run();
            if computer.get_memory(0) == 19690720 {
                return 100 * noun + verb;
            }
        }
    }
    0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_part_1() {
        assert_eq!(part_1("1,9,10,3,2,3,11,0,99,30,40,50", false), 3500);
        assert_eq!(part_1("1,0,0,0,99", false), 2);
        assert_eq!(part_1("2,3,0,3,99", false), 2);
        assert_eq!(part_1("2,4,4,5,99,0", false), 2);
        assert_eq!(part_1("1,1,1,4,99,5,6,0,99", false), 30);
    }

    #[test]
    fn test_part_1_actual_input() {
        let result = part_1(
            &fs::read_to_string("src/bin/day-02/input.txt").unwrap(),
            true,
        );
        assert_eq!(result, 2890696);
    }

    #[test]
    fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-02/input.txt").unwrap());
        assert_eq!(result, 8226);
    }
}
