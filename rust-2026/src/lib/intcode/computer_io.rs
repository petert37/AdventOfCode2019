use tokio::sync::mpsc::{self, Receiver, Sender};

pub struct StringInput {
    string: String,
    sender: Sender<i64>,
}

impl StringInput {
    pub fn new(input: String) -> (Self, Receiver<i64>) {
        let (sender, receiver) = mpsc::channel(32);
        (
            StringInput {
                string: input,
                sender,
            },
            receiver,
        )
    }

    pub async fn run(&self) {
        for line in self.string.lines() {
            let value = line
                .parse::<i64>()
                .map_err(|err| format!("Invalid input {}: {}", line, err))
                .unwrap();
            self.sender.send(value).await.unwrap();
        }
    }
}

pub struct StringOutput {
    string: String,
    receiver: Receiver<i64>,
}

impl StringOutput {
    pub fn new() -> (Self, Sender<i64>) {
        let (sender, receiver) = mpsc::channel(32);
        (
            StringOutput {
                string: String::new(),
                receiver,
            },
            sender,
        )
    }

    pub async fn run(&mut self) -> String {
        while let Some(value) = self.receiver.recv().await {
            self.string.push_str(&value.to_string());
            self.string.push('\n');
        }
        self.string.clone()
    }
}
