#[derive(Clone)]
pub struct IntcodeComputer {
    memory: Vec<i64>,
    instruction_pointer: usize,
}

impl IntcodeComputer {
    pub fn new(input: &str) -> IntcodeComputer {
        let memory = input
            .split(',')
            .map(|num| num.parse::<i64>().unwrap())
            .collect::<Vec<_>>();
        IntcodeComputer {
            memory,
            instruction_pointer: 0,
        }
    }

    pub fn run(&mut self) {
        loop {
            let instruction =
                Instruction::from_memory_address(&self.memory, self.instruction_pointer);
            match instruction {
                Instruction::Halt => break,
                _ => self.execute_instruction(&instruction),
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

    fn execute_instruction(&mut self, instruction: &Instruction) {
        match instruction {
            Instruction::Add { lhs, rhs, dst } => {
                let result = self.get_memory(*lhs as usize) + self.get_memory(*rhs as usize);
                self.set_memory(*dst as usize, result);
                self.move_instruction_pointer(4);
            }
            Instruction::Multiply { lhs, rhs, dst } => {
                let result = self.get_memory(*lhs as usize) * self.get_memory(*rhs as usize);
                self.set_memory(*dst as usize, result);
                self.move_instruction_pointer(4);
            }
            Instruction::Halt => (),
        }
    }

    fn move_instruction_pointer(&mut self, offset: i64) {
        if offset > 0 {
            self.instruction_pointer += offset as usize;
        } else {
            self.instruction_pointer -= offset.unsigned_abs() as usize;
        }
    }
}

enum Instruction {
    Add { lhs: i64, rhs: i64, dst: i64 },
    Multiply { lhs: i64, rhs: i64, dst: i64 },
    Halt,
}

impl Instruction {
    fn from_memory_address(memory: &[i64], address: usize) -> Instruction {
        let opcode = memory[address];
        match opcode {
            1 => Instruction::Add {
                lhs: memory[address + 1],
                rhs: memory[address + 2],
                dst: memory[address + 3],
            },
            2 => Instruction::Multiply {
                lhs: memory[address + 1],
                rhs: memory[address + 2],
                dst: memory[address + 3],
            },
            99 => Instruction::Halt,
            _ => panic!("Invalid opcode: {}", opcode),
        }
    }
}
