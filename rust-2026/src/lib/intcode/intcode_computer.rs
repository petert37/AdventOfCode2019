pub struct Computer {
    program_counter: u64,
    memory: Vec<i64>,
}

impl Computer {
    pub fn new() -> Self {
        Computer {
            program_counter: 0,
            memory: vec![],
        }
    }

    pub fn load_memory(&mut self, memory: Vec<i64>) {
        self.memory = memory;
    }

    pub fn get_memory(&self) -> &[i64] {
        &self.memory
    }

    pub fn run(&mut self) {
        loop {
            let instruction = self.get_instruction();
            if matches!(instruction, Instruction::Halt) {
                break;
            }
            instruction.execute(self);
        }
    }

    fn get_pointer_address(&self, address: u64) -> u64 {
        let pointer_address = self.memory[address as usize];
        if pointer_address < 0 {
            panic!(
                "Invalid pointer address at {}: {}",
                address, pointer_address
            );
        }
        pointer_address as u64
    }

    fn get_pointer_value(&self, address: u64) -> i64 {
        self.memory[address as usize]
    }

    fn set_pointer_value(&mut self, address: u64, value: i64) {
        self.memory[address as usize] = value;
    }

    fn move_program_counter(&mut self, amount: u64) {
        self.program_counter += amount;
    }

    fn get_instruction(&self) -> Instruction {
        let address = self.program_counter as usize;
        let opcode = self.memory[address];
        match opcode {
            1 => Instruction::Add {
                operand_1_address: self.get_pointer_address(self.program_counter + 1),
                operand_2_address: self.get_pointer_address(self.program_counter + 2),
                output_address: self.get_pointer_address(self.program_counter + 3),
            },
            2 => Instruction::Multiply {
                operand_1_address: self.get_pointer_address(self.program_counter + 1),
                operand_2_address: self.get_pointer_address(self.program_counter + 2),
                output_address: self.get_pointer_address(self.program_counter + 3),
            },
            99 => Instruction::Halt,
            _ => panic!("Invalid instruction at {}: {}", address, opcode),
        }
    }
}

impl Default for Computer {
    fn default() -> Self {
        Self::new()
    }
}

enum Instruction {
    Add {
        operand_1_address: u64,
        operand_2_address: u64,
        output_address: u64,
    },
    Multiply {
        operand_1_address: u64,
        operand_2_address: u64,
        output_address: u64,
    },
    Halt,
}

impl Instruction {
    fn execute(&self, computer: &mut Computer) {
        match self {
            Instruction::Add {
                operand_1_address,
                operand_2_address,
                output_address,
            } => {
                let result = computer.get_pointer_value(*operand_1_address)
                    + computer.get_pointer_value(*operand_2_address);
                computer.set_pointer_value(*output_address, result);
                computer.move_program_counter(4);
            }
            Instruction::Multiply {
                operand_1_address,
                operand_2_address,
                output_address,
            } => {
                let result = computer.get_pointer_value(*operand_1_address)
                    * computer.get_pointer_value(*operand_2_address);
                computer.set_pointer_value(*output_address, result);
                computer.move_program_counter(4);
            }
            Instruction::Halt => panic!("Cannot execute Halt instruction"),
        }
    }
}
