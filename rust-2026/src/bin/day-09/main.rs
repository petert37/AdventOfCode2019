use intcode::{input::read_input, intcode_computer::run_program_with_input};

#[tokio::main]
async fn main() {
    let input = read_input(9);
    let result = part_1(&input).await;
    println!("Part 1: {}", result);
    let result = part_2(&input).await;
    println!("Part 2: {}", result);
}

async fn part_1(program: &str) -> String {
    run_program_with_input(program, "1").await
}

async fn part_2(program: &str) -> String {
    run_program_with_input(program, "2").await
}

#[cfg(test)]
mod test {
    use intcode::input::read_input;

    use crate::{part_1, part_2};

    #[tokio::test]
    async fn test_part_1() {
        let input = read_input(9);
        let result = part_1(&input).await;
        assert_eq!(result.trim(), "3380552333".to_string())
    }

    #[tokio::test]
    async fn test_part_2() {
        let input = read_input(9);
        let result = part_2(&input).await;
        assert_eq!(result.trim(), "78831".to_string())
    }
}
