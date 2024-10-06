package experiments.regfile

import chisel3._
import chisel3.util._

class RegfileReadBundle extends Bundle {
    val raddr = Input(UInt(5.W))
    val rdata = Output(UInt(32.W))
}

class RegfileWriteBundle extends Bundle {
    val waddr = Input(UInt(5.W))
    val wdata = Input(UInt(32.W))
}

class Regfile extends Module {
    val io = IO(new Bundle {
        val we = Input(Bool())
    })
    val registers = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
    val read_a = IO(new RegfileReadBundle)
    val read_b = IO(new RegfileReadBundle)
    val write = IO(new RegfileWriteBundle)
    
    read_a.rdata := registers(read_a.raddr)
    read_b.rdata := registers(read_b.raddr)

    withClock(clock) {
        when(io.we) {
            registers(write.waddr) := write.wdata
        }
    }
}
