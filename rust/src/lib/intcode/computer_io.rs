use tokio::sync::mpsc::{Receiver, Sender};

use tokio::io::{AsyncBufReadExt, BufReader};

pub struct StdInComputerInput {
    computer_input: Sender<i64>,
}

impl StdInComputerInput {
    pub fn new() -> (Self, Receiver<i64>) {
        let (sender, receiver) = tokio::sync::mpsc::channel(1);
        (
            StdInComputerInput {
                computer_input: sender,
            },
            receiver,
        )
    }

    pub async fn run(&self) {
        let stdin = tokio::io::stdin();
        let reader = BufReader::new(stdin);
        let mut lines = reader.lines();
        while let Some(line) = lines.next_line().await.unwrap() {
            let input = line.parse::<i64>().unwrap();
            self.computer_input.send(input).await.unwrap();
        }
    }
}

pub struct StdOutComputerOutput {
    computer_output: Receiver<i64>,
}

impl StdOutComputerOutput {
    pub fn new() -> (Self, Sender<i64>) {
        let (sender, receiver) = tokio::sync::mpsc::channel(1);
        (
            StdOutComputerOutput {
                computer_output: receiver,
            },
            sender,
        )
    }

    pub async fn run(&mut self) {
        while let Some(output) = self.computer_output.recv().await {
            println!("{}", output);
        }
    }
}

pub struct StringComputerInput {
    computer_input: Sender<i64>,
    input: String,
}

impl StringComputerInput {
    pub fn new(input: String) -> (Self, Receiver<i64>) {
        let (sender, receiver) = tokio::sync::mpsc::channel(1);
        (
            StringComputerInput {
                computer_input: sender,
                input,
            },
            receiver,
        )
    }

    pub async fn run(&self) {
        for line in self.input.lines() {
            let input = line.parse::<i64>().unwrap();
            self.computer_input.send(input).await.unwrap();
        }
    }
}

pub struct StringComputerOutput {
    computer_output: Receiver<i64>,
    output: String,
}

impl StringComputerOutput {
    pub fn new() -> (Self, Sender<i64>) {
        let (sender, receiver) = tokio::sync::mpsc::channel(1);
        (
            StringComputerOutput {
                computer_output: receiver,
                output: String::new(),
            },
            sender,
        )
    }

    pub async fn run(&mut self) -> String {
        while let Some(output) = self.computer_output.recv().await {
            self.output.push_str(&output.to_string());
        }
        self.output.clone()
    }
}
