use std::fs;

use intcode::intcode_computer::run_program;

#[tokio::main]
async fn main() {
    let part_1_result = part_1(&fs::read_to_string("src/bin/day-09/input.txt").unwrap()).await;
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-09/input.txt").unwrap()).await;
    println!("Part 2 result: {}", part_2_result);
}

async fn part_1(input: &str) -> i64 {
    run_program(input, "1").await.parse::<i64>().unwrap()
}

async fn part_2(input: &str) -> i64 {
    run_program(input, "2").await.parse::<i64>().unwrap()
}

#[cfg(test)]
mod tests {
    use intcode::intcode_computer::run_program;

    use super::*;

    #[tokio::test]
    async fn test_part_1_actual_input() {
        let result = part_1(&fs::read_to_string("src/bin/day-09/input.txt").unwrap()).await;
        assert_eq!(result, 3380552333);
    }

    #[tokio::test]
    async fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-09/input.txt").unwrap()).await;
        assert_eq!(result, 78831);
    }

    #[tokio::test]
    async fn test_1() {
        let program = "109,1,204,-1,1001,100,1,100,1008,100,16,101,1006,101,0,99";
        let result = run_program(program, "").await;
        let exptected = program.replace(",", "");
        assert_eq!(result, exptected);
    }

    #[tokio::test]
    async fn test_2() {
        let result = run_program("1102,34915192,34915192,7,4,7,99,0", "").await;
        assert!(result.len() == 16);
    }

    #[tokio::test]
    async fn position_mode_equal_8_more() {
        let result = run_program("104,1125899906842624,99", "")
            .await
            .parse::<i64>()
            .unwrap();
        assert_eq!(result, 1125899906842624);
    }
}
