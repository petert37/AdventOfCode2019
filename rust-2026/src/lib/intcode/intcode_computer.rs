use tokio::sync::mpsc::{self, Receiver, Sender};

use crate::{
    computer_io::{StringInput, StringOutput},
    input::parse_input,
};

pub struct Computer {
    instruction_pointer: u64,
    memory: Vec<i64>,
    relative_base: u64,
    input: Receiver<i64>,
    output: Sender<i64>,
}

impl Computer {
    pub fn new() -> Self {
        let (_, input) = mpsc::channel(32);
        let (output, _) = mpsc::channel(32);
        Computer {
            instruction_pointer: 0,
            memory: vec![],
            relative_base: 0,
            input,
            output,
        }
    }

    pub fn new_with_io(input: Receiver<i64>, output: Sender<i64>) -> Self {
        Computer {
            instruction_pointer: 0,
            memory: vec![],
            relative_base: 0,
            input,
            output,
        }
    }

    pub fn load_memory(&mut self, memory: Vec<i64>) {
        self.memory = memory;
    }

    pub fn get_memory(&self) -> &[i64] {
        &self.memory
    }

    pub fn get_memory_value(&self, address: u64) -> i64 {
        self.memory.get(address as usize).copied().unwrap_or(0)
    }

    pub fn set_memory_value(&mut self, address: u64, value: i64) {
        if address >= self.memory.len() as u64 {
            self.memory.resize((address + 1) as usize, 0);
        }
        self.memory[address as usize] = value;
    }

    pub async fn run(&mut self) {
        loop {
            let instruction = self.get_instruction();
            if matches!(instruction, Instruction::Halt) {
                break;
            }
            instruction.execute(self).await;
        }
    }

    fn move_instruction_pointer(&mut self, amount: i64) {
        let new_value = self.instruction_pointer as i64 + amount;
        if new_value < 0 {
            panic!("Instruction pointer cannot be negative")
        }
        self.instruction_pointer = new_value as u64;
    }

    fn set_instruction_pointer(&mut self, value: i64) {
        if value < 0 {
            panic!("Instruction pointer cannot be negative")
        }
        self.instruction_pointer = value as u64;
    }

    fn move_relative_base(&mut self, offset: i64) {
        self.relative_base = self.relative_base.strict_add_signed(offset)
    }

    fn get_instruction(&self) -> Instruction {
        Instruction::parse(&self.memory, self.instruction_pointer)
    }

    async fn read_input(&mut self) -> i64 {
        self.input.recv().await.unwrap()
    }

    async fn write_output(&self, value: i64) {
        self.output.send(value).await.unwrap()
    }
}

impl Default for Computer {
    fn default() -> Self {
        Self::new()
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
    Input {
        dst: Parameter,
    },
    Output {
        src: Parameter,
    },
    JumpIfTrue {
        test_value: Parameter,
        jump_target: Parameter,
    },
    JumpIfFalse {
        test_value: Parameter,
        jump_target: Parameter,
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
    RelativeBaseOffset {
        offset: Parameter,
    },
    Halt,
}

impl Instruction {
    fn parse(memory: &[i64], address: u64) -> Self {
        let address = address as usize;
        let instruction = memory[address];
        let opcode = instruction % 100;
        match opcode {
            1 => Instruction::Add {
                lhs: Parameter::parse(instruction, memory[address + 1], 0),
                rhs: Parameter::parse(instruction, memory[address + 2], 1),
                dst: Parameter::parse(instruction, memory[address + 3], 2),
            },
            2 => Instruction::Multiply {
                lhs: Parameter::parse(instruction, memory[address + 1], 0),
                rhs: Parameter::parse(instruction, memory[address + 2], 1),
                dst: Parameter::parse(instruction, memory[address + 3], 2),
            },
            3 => Instruction::Input {
                dst: Parameter::parse(instruction, memory[address + 1], 0),
            },
            4 => Instruction::Output {
                src: Parameter::parse(instruction, memory[address + 1], 0),
            },
            5 => Instruction::JumpIfTrue {
                test_value: Parameter::parse(instruction, memory[address + 1], 0),
                jump_target: Parameter::parse(instruction, memory[address + 2], 1),
            },
            6 => Instruction::JumpIfFalse {
                test_value: Parameter::parse(instruction, memory[address + 1], 0),
                jump_target: Parameter::parse(instruction, memory[address + 2], 1),
            },
            7 => Instruction::LessThan {
                lhs: Parameter::parse(instruction, memory[address + 1], 0),
                rhs: Parameter::parse(instruction, memory[address + 2], 1),
                dst: Parameter::parse(instruction, memory[address + 3], 2),
            },
            8 => Instruction::Equals {
                lhs: Parameter::parse(instruction, memory[address + 1], 0),
                rhs: Parameter::parse(instruction, memory[address + 2], 1),
                dst: Parameter::parse(instruction, memory[address + 3], 2),
            },
            9 => Instruction::RelativeBaseOffset {
                offset: Parameter::parse(instruction, memory[address + 1], 0),
            },
            99 => Instruction::Halt,
            _ => panic!(
                "Invalid instruction at {}: {} - {}",
                address, instruction, opcode
            ),
        }
    }

    async fn execute(&self, computer: &mut Computer) {
        match self {
            Instruction::Add { lhs, rhs, dst } => {
                let result = lhs.get_value(computer) + rhs.get_value(computer);
                dst.set_value(computer, result);
                computer.move_instruction_pointer(4);
            }
            Instruction::Multiply { lhs, rhs, dst } => {
                let result = lhs.get_value(computer) * rhs.get_value(computer);
                dst.set_value(computer, result);
                computer.move_instruction_pointer(4);
            }
            Instruction::Input { dst } => {
                let input = computer.read_input().await;
                dst.set_value(computer, input);
                computer.move_instruction_pointer(2);
            }
            Instruction::Output { src } => {
                let value = src.get_value(computer);
                computer.write_output(value).await;
                computer.move_instruction_pointer(2);
            }
            Instruction::JumpIfTrue {
                test_value,
                jump_target,
            } => {
                let value = test_value.get_value(computer);
                if value != 0 {
                    let target = jump_target.get_value(computer);
                    computer.set_instruction_pointer(target);
                } else {
                    computer.move_instruction_pointer(3);
                }
            }
            Instruction::JumpIfFalse {
                test_value,
                jump_target,
            } => {
                let value = test_value.get_value(computer);
                if value == 0 {
                    let target = jump_target.get_value(computer);
                    computer.set_instruction_pointer(target);
                } else {
                    computer.move_instruction_pointer(3);
                }
            }
            Instruction::LessThan { lhs, rhs, dst } => {
                let value = if lhs.get_value(computer) < rhs.get_value(computer) {
                    1
                } else {
                    0
                };
                dst.set_value(computer, value);
                computer.move_instruction_pointer(4);
            }
            Instruction::Equals { lhs, rhs, dst } => {
                let value = if lhs.get_value(computer) == rhs.get_value(computer) {
                    1
                } else {
                    0
                };
                dst.set_value(computer, value);
                computer.move_instruction_pointer(4);
            }
            Instruction::RelativeBaseOffset { offset } => {
                computer.move_relative_base(offset.get_value(computer));
                computer.move_instruction_pointer(2);
            }
            Instruction::Halt => panic!("Cannot execute Halt instruction"),
        }
    }
}

enum ParameterMode {
    Position,
    Immediate,
    Relative,
}

struct Parameter {
    mode: ParameterMode,
    value: i64,
}

impl Parameter {
    fn parse(instruction: i64, value: i64, index: u32) -> Self {
        let mode_value = (instruction / 10_i64.pow(index + 2)) % 10;
        let mode = match mode_value {
            0 => ParameterMode::Position,
            1 => ParameterMode::Immediate,
            2 => ParameterMode::Relative,
            _ => panic!(
                "Invalid parameter mode: instruction: {}, index: {}, mode: {}",
                instruction, index, mode_value
            ),
        };
        Parameter { mode, value }
    }

    fn get_value(&self, computer: &Computer) -> i64 {
        match self.mode {
            ParameterMode::Immediate => self.value,
            ParameterMode::Position => {
                if self.value < 0 {
                    panic!("Invalid pointer address: {}", self.value);
                }
                computer.get_memory_value(self.value as u64)
            }
            ParameterMode::Relative => {
                computer.get_memory_value(computer.relative_base.strict_add_signed(self.value))
            }
        }
    }

    fn set_value(&self, computer: &mut Computer, value: i64) {
        match self.mode {
            ParameterMode::Immediate => {
                panic!("Dst parameter cannot be immediate mode: {}", self.value)
            }
            ParameterMode::Position => {
                if self.value < 0 {
                    panic!("Invalid pointer address: {}", self.value);
                }
                computer.set_memory_value(self.value as u64, value);
            }
            ParameterMode::Relative => {
                computer
                    .set_memory_value(computer.relative_base.strict_add_signed(self.value), value);
            }
        }
    }
}

pub async fn run_program_with_input(program: &str, input: &str) -> String {
    let program = parse_input(program);
    let (string_input, receiver) = StringInput::new(input.to_string());
    let (mut string_output, sender) = StringOutput::new();
    let mut computer = Computer::new_with_io(receiver, sender);
    computer.load_memory(program);

    let input_handle = tokio::spawn(async move {
        string_input.run().await;
    });
    let output_handle = tokio::spawn(async move { string_output.run().await });

    computer.run().await;

    drop(computer);
    input_handle.abort();

    output_handle.await.unwrap()
}
