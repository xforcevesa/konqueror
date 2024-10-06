package konqueror.frontend.pred

import chisel3._
import chisel3.util._

class BTB(BTBNUM: Int, RASNUM: Int) extends Module {
    val io = IO(new Bundle {
        val clk           = Input(Clock())
        val reset         = Input(Bool())

        // From/ To IF
        val fetch_pc      = Input(UInt(32.W))
        val fetch_en      = Input(Bool())
        val ret_pc        = Output(UInt(32.W))
        val taken         = Output(Bool())
        val ret_en        = Output(Bool())
        val ret_index     = Output(UInt(log2Ceil(BTBNUM).W))

        // From ID
        val operate_en    = Input(Bool())
        val operate_index = Input(UInt(log2Ceil(BTBNUM).W))
        val operate_pc    = Input(UInt(32.W))
        val pop_ras       = Input(Bool())
        val push_ras      = Input(Bool())

        val add_entry     = Input(Bool())
        val delete_entry  = Input(Bool())
        val pre_error     = Input(Bool())
        val pre_right     = Input(Bool())
        val target_error  = Input(Bool())
        val right_orien   = Input(Bool())
        val right_target  = Input(UInt(32.W))
    })
    val btb_pc = RegInit(VecInit(Seq.fill(BTBNUM)(0.U(30.W))))
    val btb_target = RegInit(VecInit(Seq.fill(BTBNUM)(0.U(30.W))))
    val btb_counter = RegInit(VecInit(Seq.fill(BTBNUM)(0.U(2.W))))
    val btb_valid = RegInit(VecInit(Seq.fill(BTBNUM)(false.B)))
    val ras_pc = RegInit(VecInit(Seq.fill(RASNUM)(0.U(30.W))))
    val ras_valid = RegInit(VecInit(Seq.fill(RASNUM)(false.B)))

    val fetch_pc_r = Reg(UInt(32.W))
    val fetch_en_r = Reg(Bool())
    val ras = Reg(Vec(8, UInt(30.W)))
    val ras_ptr = Reg(UInt(log2Ceil(ras.length).W))
    val ras_top = Reg(UInt(30.W))
    val ras_full = Reg(Bool())
    val ras_empty = Reg(Bool())

    val btb_match_rd = Reg(UInt(32.W))
    val ras_match_rd = Reg(UInt(32.W))

    val btb_match = Wire(Bool())
    val ras_match = Wire(Bool())

    val btb_match_target = Wire(UInt(30.W))
    val btb_match_counter = Wire(UInt(2.W))

    val btb_match_index = Wire(UInt(log2Ceil(BTBNUM).W))
    val ras_match_index = Wire(UInt(log2Ceil(RASNUM).W))

    val btb_add_entry_dec = Wire(UInt(32.W))
    val btb_add_entry_index = Wire(UInt(log2Ceil(BTBNUM).W))
    val btb_add_entry_index_r = Reg(UInt(log2Ceil(BTBNUM).W))
    val btb_all_entry_valid = Wire(Bool())
    val btb_select_one_invalid_entry = Wire(UInt(5.W))
}
