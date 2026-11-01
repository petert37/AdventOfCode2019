use std::fs;

pub fn read_input(day: u32) -> String {
    let file = format!("src/bin/day-{:02}/input.txt", day);
    fs::read_to_string(file).unwrap()
}

pub fn parse_input(input: &str) -> Vec<i64> {
    input
        .split(",")
        .map(|value| {
            value
                .parse::<i64>()
                .map_err(|err| format!("Invalid input: {}: {}", value, err))
                .unwrap()
        })
        .collect()
}
