use std::fs;

use intcode::intcode_computer::IntcodeComputer;

#[tokio::main]
async fn main() {
    let part_1_result = part_1(
        &fs::read_to_string("src/bin/day-02/input.txt").unwrap(),
        true,
    )
    .await;
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-02/input.txt").unwrap()).await;
    println!("Part 2 result: {}", part_2_result);
}

async fn part_1(input: &str, modify: bool) -> i64 {
    let mut computer = IntcodeComputer::new(input);
    if modify {
        computer.set_memory(1, 12);
        computer.set_memory(2, 2);
    }
    computer.run().await;
    computer.get_memory(0)
}

async fn part_2(input: &str) -> i64 {
    let mut computer = IntcodeComputer::new(input);
    let memory_snapshot = computer.get_memory_snapshot();
    for noun in 0..100 {
        for verb in 0..100 {
            computer.restore_memory_snapshot(memory_snapshot.clone());
            computer.reset_instruction_pointer();
            computer.set_memory(1, noun);
            computer.set_memory(2, verb);
            computer.run().await;
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

    #[tokio::test]
    async fn test_part_1() {
        assert_eq!(part_1("1,9,10,3,2,3,11,0,99,30,40,50", false).await, 3500);
        assert_eq!(part_1("1,0,0,0,99", false).await, 2);
        assert_eq!(part_1("2,3,0,3,99", false).await, 2);
        assert_eq!(part_1("2,4,4,5,99,0", false).await, 2);
        assert_eq!(part_1("1,1,1,4,99,5,6,0,99", false).await, 30);
    }

    #[tokio::test]
    async fn test_part_1_actual_input() {
        let result = part_1(
            &fs::read_to_string("src/bin/day-02/input.txt").unwrap(),
            true,
        )
        .await;
        assert_eq!(result, 2890696);
    }

    #[tokio::test]
    async fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-02/input.txt").unwrap()).await;
        assert_eq!(result, 8226);
    }
}
