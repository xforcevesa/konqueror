package experiments.minicpu

import chisel3._
import chisel3.util._
import experiments.regfile.Regfile

class MiniCPUTop extends Module {
  val io = IO(new Bundle {
    val inst_sram_we = Output(Bool())
    val inst_sram_addr = Output(UInt(32.W))
    val inst_sram_wdata = Output(UInt(32.W))
    val inst_sram_rdata = Input(UInt(32.W))

    val data_sram_we = Output(Bool())
    val data_sram_addr = Output(UInt(32.W))
    val data_sram_wdata = Output(UInt(32.W))
    val data_sram_rdata = Input(UInt(32.W))

    val debug_pc = Output(UInt(32.W))
    val debug_inst = Output(UInt(3.W))
  })

  val valid = RegInit(false.B)

  withClock(clock) {
    when(reset.asBool) {
      valid := false.B
    }.otherwise {
      valid := true.B
    }
  }

  val pc = RegInit(0x1bfffffc.U(32.W))
  val nextpc = WireDefault(pc + 4.U)

  val inst = io.inst_sram_rdata

  val rd = inst(4, 0)
  val rj = inst(9, 5)
  val rk = inst(14, 10)
  val i12 = inst(21, 10)
  val i16 = inst(25, 10)

  // Instruction decoding
  val inst_add_w = Lookup(inst, false.B, Seq(Instructions.ADDW -> true.B))
  val inst_addi_w = Lookup(inst, false.B, Seq(Instructions.ADDIW -> true.B))
  val inst_ld_w = Lookup(inst, false.B, Seq(Instructions.LDW -> true.B))
  val inst_st_w = Lookup(inst, false.B, Seq(Instructions.STW -> true.B))
  val inst_bne = Lookup(inst, false.B, Seq(Instructions.BNE -> true.B))

  val src2_is_imm = inst_addi_w || inst_ld_w || inst_st_w
  val res_from_mem = inst_ld_w
  val gr_we = (inst_add_w || inst_ld_w || inst_addi_w)
  val mem_we = inst_st_w
  val src_reg_is_rd = inst_bne || inst_st_w

  val rf_raddr1 = rj
  val rf_raddr2 = Mux(src_reg_is_rd, rd, rk)
  val rf_wdata = Wire(UInt(32.W))

  val regfile = Module(new Regfile)
  regfile.io.we:= gr_we
  regfile.read_a.raddr := rf_raddr1
  regfile.read_b.raddr := rf_raddr2
  regfile.write.waddr := rd
  regfile.write.wdata := rf_wdata
  regfile.clock.connect(clock)
  regfile.reset := reset

  val rj_value = regfile.read_a.rdata
  val rkd_value = regfile.read_b.rdata

  val br_offs = Cat(Fill(14, i16(15)), i16, "b00".U(2.W))
  val br_target = pc + br_offs
  val rj_eq_rd = rj_value === rkd_value
  val br_taken = valid && inst_bne && !rj_eq_rd

  nextpc := Mux(br_taken, br_target, pc + 4.U)

  val imm = Cat(Fill(20, i12(11)), i12)
  val alu_src1 = rj_value
  val alu_src2 = Mux(src2_is_imm, imm, rkd_value)
  val alu_result = alu_src1 + alu_src2

  withClock(clock) {
    when(reset.asBool) {
      pc := 0x1bfffffc.U
    }.otherwise {
      pc := nextpc
    }
  }

  io.inst_sram_we := false.B
  io.inst_sram_addr := pc
  io.inst_sram_wdata := 0.U
  rf_wdata := Mux(res_from_mem, io.data_sram_rdata, alu_result)

  io.data_sram_we := mem_we
  io.data_sram_addr := alu_result
  io.data_sram_wdata := rkd_value

  io.debug_pc := pc
  io.debug_inst := Lookup(inst, 7.U, Seq(
    Instructions.ADDW -> 0.U,
    Instructions.ADDIW -> 1.U,
    Instructions.LDW -> 2.U,
    Instructions.STW -> 3.U,
    Instructions.BNE -> 4.U
  ))
}
