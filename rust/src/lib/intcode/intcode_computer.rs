use tokio::sync::mpsc::{Receiver, Sender};

use crate::computer_io::{StringComputerInput, StringComputerOutput};

pub struct IntcodeComputer {
    memory: Vec<i64>,
    instruction_pointer: usize,
    input: Option<Receiver<i64>>,
    output: Option<Sender<i64>>,
}

impl IntcodeComputer {
    pub fn new(input: &str) -> IntcodeComputer {
        let memory = IntcodeComputer::parse_memory(input);
        IntcodeComputer {
            memory,
            instruction_pointer: 0,
            input: None,
            output: None,
        }
    }

    pub fn new_with_io(
        input: &str,
        computer_input: Receiver<i64>,
        computer_output: Sender<i64>,
    ) -> Self {
        let memory = IntcodeComputer::parse_memory(input);
        IntcodeComputer {
            memory,
            instruction_pointer: 0,
            input: Some(computer_input),
            output: Some(computer_output),
        }
    }

    fn parse_memory(input: &str) -> Vec<i64> {
        input
            .split(',')
            .map(|num| num.parse::<i64>().unwrap())
            .collect::<Vec<_>>()
    }

    pub async fn run(&mut self) {
        loop {
            let instruction =
                Instruction::from_memory_address(&self.memory, self.instruction_pointer);
            match instruction {
                Instruction::Halt => break,
                _ => self.execute_instruction(&instruction).await,
            }
        }
    }

    pub fn set_memory(&mut self, address: usize, value: i64) {
        if address >= self.memory.len() {
            self.memory.resize(address + 1, 0);
        }
        self.memory[address] = value;
    }

    pub fn get_memory(&self, address: usize) -> i64 {
        if address >= self.memory.len() {
            0
        } else {
            self.memory[address]
        }
    }

    async fn execute_instruction(&mut self, instruction: &Instruction) {
        match instruction {
            Instruction::Add { lhs, rhs, dst } => {
                let result = lhs.get_value(&self.memory) + rhs.get_value(&self.memory);
                self.set_memory(dst.get_dst_address(), result);
                self.move_instruction_pointer(4);
            }
            Instruction::Multiply { lhs, rhs, dst } => {
                let result = lhs.get_value(&self.memory) * rhs.get_value(&self.memory);
                self.set_memory(dst.get_dst_address(), result);
                self.move_instruction_pointer(4);
            }
            Instruction::Halt => (),
            Instruction::Input { dst } => {
                let read_data = self
                    .input
                    .as_mut()
                    .expect("Input channel not set")
                    .recv()
                    .await
                    .expect("Failed to read input");
                self.set_memory(dst.get_dst_address(), read_data);
                self.move_instruction_pointer(2);
            }
            Instruction::Output { src } => {
                let output_data = src.get_value(&self.memory);
                self.output
                    .as_mut()
                    .expect("Output channel not set")
                    .send(output_data)
                    .await
                    .expect("Failed to send output");
                self.move_instruction_pointer(2);
            }
            Instruction::JumpIfTrue {
                condition,
                jump_address,
            } => {
                if condition.get_value(&self.memory) != 0 {
                    self.instruction_pointer = jump_address.get_value(&self.memory) as usize;
                } else {
                    self.move_instruction_pointer(3);
                }
            }
            Instruction::JumpIfFalse {
                condition,
                jump_address,
            } => {
                if condition.get_value(&self.memory) == 0 {
                    self.instruction_pointer = jump_address.get_value(&self.memory) as usize;
                } else {
                    self.move_instruction_pointer(3);
                }
            }
            Instruction::LessThan { lhs, rhs, dst } => {
                let result = if lhs.get_value(&self.memory) < rhs.get_value(&self.memory) {
                    1
                } else {
                    0
                };
                self.set_memory(dst.get_dst_address(), result);
                self.move_instruction_pointer(4);
            }
            Instruction::Equals { lhs, rhs, dst } => {
                let result = if lhs.get_value(&self.memory) == rhs.get_value(&self.memory) {
                    1
                } else {
                    0
                };
                self.set_memory(dst.get_dst_address(), result);
                self.move_instruction_pointer(4);
            }
        }
    }

    fn move_instruction_pointer(&mut self, offset: i64) {
        if offset > 0 {
            self.instruction_pointer += offset as usize;
        } else {
            self.instruction_pointer -= offset.unsigned_abs() as usize;
        }
    }

    pub fn get_memory_snapshot(&self) -> Vec<i64> {
        self.memory.clone()
    }

    pub fn restore_memory_snapshot(&mut self, snapshot: Vec<i64>) {
        self.memory = snapshot;
    }

    pub fn reset_instruction_pointer(&mut self) {
        self.instruction_pointer = 0;
    }
}

enum Instruction {
    Add {
        lhs: Parameter,
        rhs: Parameter,
        dst: Parameter,
    },
    Multiply {
        lhs: Parameter,
        rhs: Parameter,
        dst: Parameter,
    },
    Halt,
    Input {
        dst: Parameter,
    },
    Output {
        src: Parameter,
    },
    JumpIfTrue {
        condition: Parameter,
        jump_address: Parameter,
    },
    JumpIfFalse {
        condition: Parameter,
        jump_address: Parameter,
    },
    LessThan {
        lhs: Parameter,
        rhs: Parameter,
        dst: Parameter,
    },
    Equals {
        lhs: Parameter,
        rhs: Parameter,
        dst: Parameter,
    },
}

impl Instruction {
    fn from_memory_address(memory: &[i64], address: usize) -> Instruction {
        let value = memory[address];
        let opcode = value % 100;
        match opcode {
            1 => Instruction::Add {
                lhs: Parameter::from_memory(memory, address, 0),
                rhs: Parameter::from_memory(memory, address, 1),
                dst: Parameter::from_memory(memory, address, 2),
            },
            2 => Instruction::Multiply {
                lhs: Parameter::from_memory(memory, address, 0),
                rhs: Parameter::from_memory(memory, address, 1),
                dst: Parameter::from_memory(memory, address, 2),
            },
            99 => Instruction::Halt,
            3 => Instruction::Input {
                dst: Parameter::from_memory(memory, address, 0),
            },
            4 => Instruction::Output {
                src: Parameter::from_memory(memory, address, 0),
            },
            5 => Instruction::JumpIfTrue {
                condition: Parameter::from_memory(memory, address, 0),
                jump_address: Parameter::from_memory(memory, address, 1),
            },
            6 => Instruction::JumpIfFalse {
                condition: Parameter::from_memory(memory, address, 0),
                jump_address: Parameter::from_memory(memory, address, 1),
            },
            7 => Instruction::LessThan {
                lhs: Parameter::from_memory(memory, address, 0),
                rhs: Parameter::from_memory(memory, address, 1),
                dst: Parameter::from_memory(memory, address, 2),
            },
            8 => Instruction::Equals {
                lhs: Parameter::from_memory(memory, address, 0),
                rhs: Parameter::from_memory(memory, address, 1),
                dst: Parameter::from_memory(memory, address, 2),
            },
            _ => panic!("Invalid opcode: {}", opcode),
        }
    }
}

enum ParamtereMode {
    Position,
    Immediate,
}

impl ParamtereMode {
    fn from_memory_value(value: i64, parameter_index: usize) -> Self {
        let divisor = 10_i64.pow(parameter_index as u32 + 2);
        match (value / divisor) % 10 {
            0 => ParamtereMode::Position,
            1 => ParamtereMode::Immediate,
            _ => panic!("Invalid parameter mode: {} {}", value, parameter_index),
        }
    }
}

struct Parameter {
    mode: ParamtereMode,
    value: i64,
}

impl Parameter {
    fn from_memory(memory: &[i64], address: usize, parameter_index: usize) -> Parameter {
        Parameter {
            mode: ParamtereMode::from_memory_value(memory[address], parameter_index),
            value: memory[address + parameter_index + 1],
        }
    }

    fn get_value(&self, memory: &[i64]) -> i64 {
        match self.mode {
            ParamtereMode::Position => memory[self.value as usize],
            ParamtereMode::Immediate => self.value,
        }
    }

    fn get_dst_address(&self) -> usize {
        match self.mode {
            ParamtereMode::Position => self.value as usize,
            ParamtereMode::Immediate => panic!("Immediate mode not supported for destination"),
        }
    }
}

pub async fn run_program(program: &str, input: &str) -> String {
    let (string_input, computer_input) = StringComputerInput::new(input.to_string());
    let (mut string_output, computer_output) = StringComputerOutput::new();
    let mut computer = IntcodeComputer::new_with_io(program, computer_input, computer_output);

    let string_input_future = tokio::spawn(async move { string_input.run().await });
    let string_output_future = tokio::spawn(async move { string_output.run().await });

    computer.run().await;
    drop(computer); // Drop computer to ensure that the output future completes

    string_input_future.abort();
    string_output_future.await.unwrap()
}
