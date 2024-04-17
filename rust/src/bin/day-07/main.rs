use std::fs;

use intcode::{
    computer_io::{LoggingConnector, StringComputerOutput},
    intcode_computer::IntcodeComputer,
};
use permute::permutations_of;

#[tokio::main]
async fn main() {
    let part_1_result = part_1(&fs::read_to_string("src/bin/day-07/input.txt").unwrap()).await;
    println!("Part 1 result: {}", part_1_result);
    let part_2_result = part_2(&fs::read_to_string("src/bin/day-07/input.txt").unwrap()).await;
    println!("Part 2 result: {}", part_2_result);
}

async fn part_1(input: &str) -> i64 {
    let mut max: i64 = 0;
    for permutation in permutations_of::<i64>(&[0, 1, 2, 3, 4]) {
        let permutation = permutation.collect::<Vec<_>>();

        let (sender_0, receiver_0) = tokio::sync::mpsc::channel(1);
        let (sender_1, receiver_1) = tokio::sync::mpsc::channel(1);
        let (sender_2, receiver_2) = tokio::sync::mpsc::channel(1);
        let (sender_3, receiver_3) = tokio::sync::mpsc::channel(1);
        let (sender_4, receiver_4) = tokio::sync::mpsc::channel(1);
        let (mut thruster_output, sender_5) = StringComputerOutput::new();

        let mut computer_0 = IntcodeComputer::new_with_io(input, receiver_0, sender_1.clone());
        let mut computer_1 = IntcodeComputer::new_with_io(input, receiver_1, sender_2.clone());
        let mut computer_2 = IntcodeComputer::new_with_io(input, receiver_2, sender_3.clone());
        let mut computer_3 = IntcodeComputer::new_with_io(input, receiver_3, sender_4.clone());
        let mut computer_4 = IntcodeComputer::new_with_io(input, receiver_4, sender_5);

        tokio::spawn(async move { computer_0.run().await });
        tokio::spawn(async move { computer_1.run().await });
        tokio::spawn(async move { computer_2.run().await });
        tokio::spawn(async move { computer_3.run().await });
        tokio::spawn(async move { computer_4.run().await });

        let output_future = tokio::spawn(async move { thruster_output.run().await });

        sender_0.send(*permutation[0]).await.unwrap();
        sender_1.send(*permutation[1]).await.unwrap();
        sender_2.send(*permutation[2]).await.unwrap();
        sender_3.send(*permutation[3]).await.unwrap();
        sender_4.send(*permutation[4]).await.unwrap();

        sender_0.send(0).await.unwrap();

        let output = output_future.await.unwrap().parse().unwrap();
        if output > max {
            max = output;
        }
    }
    max
}

async fn part_2(input: &str) -> i64 {
    let mut max: i64 = 0;
    for permutation in permutations_of::<i64>(&[5, 6, 7, 8, 9]) {
        let permutation = permutation.collect::<Vec<_>>();

        let (sender_0, receiver_0) = tokio::sync::mpsc::channel(1);
        let (sender_1, receiver_1) = tokio::sync::mpsc::channel(1);
        let (sender_2, receiver_2) = tokio::sync::mpsc::channel(1);
        let (sender_3, receiver_3) = tokio::sync::mpsc::channel(1);
        let (sender_4, receiver_4) = tokio::sync::mpsc::channel(1);
        let (sender_5, receiver_5) = tokio::sync::mpsc::channel(1);

        let mut computer_0 = IntcodeComputer::new_with_io(input, receiver_0, sender_1.clone());
        let mut computer_1 = IntcodeComputer::new_with_io(input, receiver_1, sender_2.clone());
        let mut computer_2 = IntcodeComputer::new_with_io(input, receiver_2, sender_3.clone());
        let mut computer_3 = IntcodeComputer::new_with_io(input, receiver_3, sender_4.clone());
        let mut computer_4 = IntcodeComputer::new_with_io(input, receiver_4, sender_5);

        tokio::spawn(async move { computer_0.run().await });
        tokio::spawn(async move { computer_1.run().await });
        tokio::spawn(async move { computer_2.run().await });
        tokio::spawn(async move { computer_3.run().await });
        tokio::spawn(async move { computer_4.run().await });

        let mut connector = LoggingConnector::new(sender_0.clone(), receiver_5);
        let output_future = tokio::spawn(async move { connector.run().await });

        sender_0.send(*permutation[0]).await.unwrap();
        sender_1.send(*permutation[1]).await.unwrap();
        sender_2.send(*permutation[2]).await.unwrap();
        sender_3.send(*permutation[3]).await.unwrap();
        sender_4.send(*permutation[4]).await.unwrap();

        sender_0.send(0).await.unwrap();

        let output = output_future
            .await
            .unwrap()
            .lines()
            .last()
            .unwrap()
            .parse()
            .unwrap();
        if output > max {
            max = output;
        }
    }
    max
}

#[cfg(test)]
mod tests {

    use super::*;

    #[tokio::test]
    async fn test_part_1_1() {
        let result = part_1("3,15,3,16,1002,16,10,16,1,16,15,15,4,15,99,0,0").await;
        assert_eq!(result, 43210);
    }

    #[tokio::test]
    async fn test_part_1_2() {
        let result =
            part_1("3,23,3,24,1002,24,10,24,1002,23,-1,23,101,5,23,23,1,24,23,23,4,23,99,0,0")
                .await;
        assert_eq!(result, 54321);
    }

    #[tokio::test]
    async fn test_part_1_3() {
        let result = part_1("3,31,3,32,1002,32,10,32,1001,31,-2,31,1007,31,0,33,1002,33,7,33,1,33,31,31,1,32,31,31,4,31,99,0,0,0").await;
        assert_eq!(result, 65210);
    }

    #[tokio::test]
    async fn test_part_1_actual_input() {
        let result = part_1(&fs::read_to_string("src/bin/day-07/input.txt").unwrap()).await;
        assert_eq!(result, 24405);
    }

    #[tokio::test]
    async fn test_part_2_1() {
        let result = part_2(
            "3,26,1001,26,-4,26,3,27,1002,27,2,27,1,27,26,27,4,27,1001,28,-1,28,1005,28,6,99,0,0,5",
        )
        .await;
        assert_eq!(result, 139629729);
    }

    #[tokio::test]
    async fn test_part_2_2() {
        let result =
            part_2("3,52,1001,52,-5,52,3,53,1,52,56,54,1007,54,5,55,1005,55,26,1001,54,-5,54,1105,1,12,1,53,54,53,1008,54,0,55,1001,55,1,55,2,53,55,53,4,53,1001,56,-1,56,1005,56,6,99,0,0,0,0,10")
                .await;
        assert_eq!(result, 18216);
    }

    #[tokio::test]
    async fn test_part_2_actual_input() {
        let result = part_2(&fs::read_to_string("src/bin/day-07/input.txt").unwrap()).await;
        assert_eq!(result, 8271623);
    }
}
