package experiments.scroller

import chisel3._
import chisel3.util._

class Scroller(val n_cycles: Int = 1000, val n_leds: Int = 16) extends Module {
    val io = IO(new Bundle {
        val output = Output(UInt(n_leds.W))
    })

    val counter = RegInit(0.U(32.W))
    val output_reg = RegInit(1.U(n_leds.W))
    val led_ready = counter >= n_cycles.U

    withClock(clock) {
        when(reset.asBool || led_ready) {
            counter := 0.U
        }.otherwise {
            counter := counter + 1.U
        }
    }
    withClock(clock) {
        when(reset.asBool) {
            output_reg := 1.U(n_leds.W)
        }.elsewhen(led_ready) {
            output_reg := Cat(output_reg(n_leds-2, 0), output_reg(n_leds-1))
        }
    }
    io.output := output_reg
}

// Generate the Verilog 
// object Scroller extends App {
//     emitVerilog(new Scroller)
// }
