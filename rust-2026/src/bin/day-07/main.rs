use intcode::{
    input::{parse_input, read_input},
    intcode_computer::Computer,
};
use itertools::Itertools;
use tokio::sync::mpsc::{self, Receiver, Sender};

#[tokio::main]
async fn main() {
    let input = read_input(7);
    let program = parse_input(&input);
    let result = part_1(&program).await;
    println!("Part 1: {}", result);
    let result = part_2(&program).await;
    println!("Part 2: {}", result);
}

async fn part_1(program: &[i64]) -> String {
    let phase_sequences = (0..=4).permutations(5).collect::<Vec<_>>();
    let mut max = 0;
    for phase_sequence in &phase_sequences {
        let result = run_amplifier_sequence(program, phase_sequence).await;
        if result > max {
            max = result;
        }
    }
    max.to_string()
}

async fn part_2(program: &[i64]) -> String {
    let phase_sequences = (5..=9).permutations(5).collect::<Vec<_>>();
    let mut max = 0;
    for phase_sequence in &phase_sequences {
        let result = run_amplifier_loop(program, phase_sequence).await;
        if result > max {
            max = result;
        }
    }
    max.to_string()
}

async fn run_amplifier_sequence(program: &[i64], phase_settings: &[i64]) -> i64 {
    let (_, mut output, computers) = make_amplifiers(program, phase_settings).await;
    let _ = computers
        .into_iter()
        .map(|mut c| tokio::spawn(async move { c.run().await }))
        .collect::<Vec<_>>();
    output.recv().await.unwrap()
}

async fn run_amplifier_loop(program: &[i64], phase_settings: &[i64]) -> i64 {
    let (input, mut output, computers) = make_amplifiers(program, phase_settings).await;
    let computers = computers
        .into_iter()
        .map(|mut c| tokio::spawn(async move { c.run().await }))
        .collect::<Vec<_>>();
    let result_handle = tokio::spawn(async move {
        let mut latest: Option<i64> = None;
        while let Some(value) = output.recv().await {
            latest = Some(value);
            let _ = input.send(value).await;
        }
        latest
    });
    for computer in computers {
        let _ = computer.await;
    }
    result_handle.await.unwrap().unwrap()
}

async fn make_amplifiers(
    program: &[i64],
    phase_settings: &[i64],
) -> (Sender<i64>, Receiver<i64>, [Computer; 5]) {
    let (sender, receiver) = mpsc::channel::<i64>(32);
    let (c0, s0, r0) = make_amplifier(program, receiver);
    let (c1, s1, r1) = make_amplifier(program, r0);
    let (c2, s2, r2) = make_amplifier(program, r1);
    let (c3, s3, r3) = make_amplifier(program, r2);
    let (c4, _, r4) = make_amplifier(program, r3);
    let _ = sender.send(phase_settings[0]).await;
    let _ = sender.send(0).await;
    let _ = s0.send(phase_settings[1]).await;
    let _ = s1.send(phase_settings[2]).await;
    let _ = s2.send(phase_settings[3]).await;
    let _ = s3.send(phase_settings[4]).await;
    (sender, r4, [c0, c1, c2, c3, c4])
}

fn make_amplifier(
    program: &[i64],
    computer_input: Receiver<i64>,
) -> (Computer, Sender<i64>, Receiver<i64>) {
    let (sender, receiver) = mpsc::channel::<i64>(32);
    let mut computer = Computer::new_with_io(computer_input, sender.clone());
    computer.load_memory(program.to_vec());
    (computer, sender, receiver)
}

#[cfg(test)]
mod test {
    use intcode::input::{parse_input, read_input};

    use crate::{part_1, part_2};

    #[tokio::test]
    async fn test_part_1() {
        let input = read_input(7);
        let program = parse_input(&input);
        let result = part_1(&program).await;
        assert_eq!(result, "24405".to_string())
    }

    #[tokio::test]
    async fn test_part_2() {
        let input = read_input(7);
        let program = parse_input(&input);
        let result = part_2(&program).await;
        assert_eq!(result, "8271623".to_string())
    }
}
