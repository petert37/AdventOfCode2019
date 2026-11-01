use intcode::{
    input::{parse_input, read_input},
    intcode_computer::Computer,
};

fn main() {
    let input = read_input(2);
    let result = part_1(&input);
    println!("Part 1: {}", result);

    let result = part_2(&input);
    println!("Part 2: {}", result);
}

fn part_1(input: &str) -> i64 {
    let mut input = parse_input(input);
    input[1] = 12;
    input[2] = 2;
    let mut computer = Computer::new();
    computer.load_memory(input);
    computer.run();
    let memory = computer.get_memory();
    memory[0]
}

fn part_2(input: &str) -> i64 {
    let expected_output = 19690720;
    let input = parse_input(input);
    for noun in 0..=99 {
        for verb in 0..=99 {
            let mut input = input.clone();
            input[1] = noun;
            input[2] = verb;
            let mut computer = Computer::new();
            computer.load_memory(input);
            computer.run();
            let memory = computer.get_memory();
            let result = memory[0];
            if result == expected_output {
                return 100 * noun + verb;
            }
        }
    }
    panic!("Result not found");
}

#[cfg(test)]
mod test {
    use intcode::input::read_input;

    use crate::{part_1, part_2};

    #[test]
    fn test_part_1() {
        let input = read_input(2);
        let result = part_1(&input);
        assert_eq!(result, 2890696);
    }

    #[test]
    fn test_part_2() {
        let input = read_input(2);
        let result = part_2(&input);
        assert_eq!(result, 8226);
    }
}
