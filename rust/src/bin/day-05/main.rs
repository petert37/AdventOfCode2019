use std::fs;

use intcode::{
    computer_io::{StdInComputerInput, StdOutComputerOutput},
    intcode_computer::{run_program, IntcodeComputer},
};

#[tokio::main]
async fn main() {
    let part_1_result = part_1(&fs::read_to_string("src/bin/day-05/input.txt").unwrap()).await;
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-05/input.txt").unwrap()).await;
    println!("Part 2 result: {}", part_2_result);
}

// Demonstrates the use of the IntcodeComputer with standard I/O
async fn _part_1_stdio(input: &str) -> i64 {
    let (stdin, computer_input) = StdInComputerInput::new();
    let (mut stdout, computer_output) = StdOutComputerOutput::new();
    let mut computer = IntcodeComputer::new_with_io(input, computer_input, computer_output);

    let stdin_future = tokio::spawn(async move { stdin.run().await });
    let stdout_future = tokio::spawn(async move { stdout.run().await });

    computer.run().await;

    stdin_future.abort();
    stdout_future.abort();
    0
}

async fn part_1(input: &str) -> i64 {
    let output = run_program(input, "1").await;
    get_diagnostic_code(&output)
}

async fn part_2(input: &str) -> i64 {
    let output = run_program(input, "5").await;
    get_diagnostic_code(&output)
}

fn get_diagnostic_code(result: &str) -> i64 {
    result.lines().last().unwrap().parse().unwrap()
}

#[cfg(test)]
mod tests {
    use intcode::intcode_computer::run_program;

    use super::*;

    #[tokio::test]
    async fn test_part_1_actual_input() {
        let result = part_1(&fs::read_to_string("src/bin/day-05/input.txt").unwrap()).await;
        assert_eq!(result, 8332629);
    }

    #[tokio::test]
    async fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-05/input.txt").unwrap()).await;
        assert_eq!(result, 8805067);
    }

    #[tokio::test]
    async fn position_mode_equal_8_less() {
        let result = run_program("3,9,8,9,10,9,4,9,99,-1,8", "7")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn position_mode_equal_8_equal() {
        let result = run_program("3,9,8,9,10,9,4,9,99,-1,8", "8")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn position_mode_equal_8_more() {
        let result = run_program("3,9,8,9,10,9,4,9,99,-1,8", "9")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn position_mode_less_8_less() {
        let result = run_program("3,9,7,9,10,9,4,9,99,-1,8", "7")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn position_mode_less_8_equal() {
        let result = run_program("3,9,7,9,10,9,4,9,99,-1,8", "8")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn position_mode_less_8_more() {
        let result = run_program("3,9,7,9,10,9,4,9,99,-1,8", "9")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn immediate_mode_equal_8_less() {
        let result = run_program("3,3,1108,-1,8,3,4,3,99", "7")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn immediate_mode_equal_8_equal() {
        let result = run_program("3,3,1108,-1,8,3,4,3,99", "8")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn immediate_mode_equal_8_more() {
        let result = run_program("3,3,1108,-1,8,3,4,3,99", "9")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn immediate_mode_less_8_less() {
        let result = run_program("3,3,1107,-1,8,3,4,3,99", "7")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn immediate_mode_less_8_equal() {
        let result = run_program("3,3,1107,-1,8,3,4,3,99", "8")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn immediate_mode_less_8_more() {
        let result = run_program("3,3,1107,-1,8,3,4,3,99", "9")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn position_mode_jump_zero() {
        let result = run_program("3,12,6,12,15,1,13,14,13,4,13,99,-1,0,1,9", "0")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn position_mode_jump_non_zero() {
        let result = run_program("3,12,6,12,15,1,13,14,13,4,13,99,-1,0,1,9", "3")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn immediate_mode_jump_zero() {
        let result = run_program("3,3,1105,-1,9,1101,0,0,12,4,12,99,1", "0")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 0);
    }

    #[tokio::test]
    async fn immediate_mode_jump_non_zero() {
        let result = run_program("3,3,1105,-1,9,1101,0,0,12,4,12,99,1", "3")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1);
    }

    #[tokio::test]
    async fn complex_below_8() {
        let result = run_program("3,21,1008,21,8,20,1005,20,22,107,8,21,20,1006,20,31,1106,0,36,98,0,0,1002,21,125,20,4,20,1105,1,46,104,999,1105,1,46,1101,1000,1,20,4,20,1105,1,46,98,99", "7")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 999);
    }

    #[tokio::test]
    async fn complex_8() {
        let result = run_program("3,21,1008,21,8,20,1005,20,22,107,8,21,20,1006,20,31,1106,0,36,98,0,0,1002,21,125,20,4,20,1105,1,46,104,999,1105,1,46,1101,1000,1,20,4,20,1105,1,46,98,99", "8")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1000);
    }

    #[tokio::test]
    async fn complex_above_8() {
        let result = run_program("3,21,1008,21,8,20,1005,20,22,107,8,21,20,1006,20,31,1106,0,36,98,0,0,1002,21,125,20,4,20,1105,1,46,104,999,1105,1,46,1101,1000,1,20,4,20,1105,1,46,98,99", "9")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1001);
    }
}
