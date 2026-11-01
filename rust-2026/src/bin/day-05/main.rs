use intcode::{input::read_input, intcode_computer::run_program_with_input};

#[tokio::main]
async fn main() {
    let input = read_input(5);
    let result = part_1(&input).await;
    println!("Part 1: {}", result);
    let result = part_2(&input).await;
    println!("Part 2: {}", result);
}

async fn part_1(input: &str) -> String {
    run_program_with_input(input, "1").await
}

async fn part_2(input: &str) -> String {
    run_program_with_input(input, "5").await
}

#[cfg(test)]
mod test {
    use intcode::input::read_input;

    use crate::{part_1, part_2};

    #[tokio::test]
    async fn test_part_1() {
        let input = read_input(5);
        let expected = r#"0
0
0
0
0
0
0
0
0
8332629
"#;
        let result = part_1(&input).await;
        assert_eq!(result, expected);
    }

    #[tokio::test]
    async fn test_part_2() {
        let input = read_input(5);
        let result = part_2(&input).await;
        assert_eq!(result, "8805067\n");
    }
}
