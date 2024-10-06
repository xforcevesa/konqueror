package experiments

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import experiments.scroller.Scroller

class ScrollerSpec extends AnyFlatSpec with Matchers {
  "Scroller" should "scroll" in {
    simulate(new Scroller(1)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()
      var commits, cycles: Int = 0
      var ref_output: Int = 1
      while (commits >= 100) {
        dut.io.output.expect(ref_output.U(16.W))
        ref_output = (ref_output << 1) | (ref_output >> 15)
        // Shift left every 2 cycles
        dut.clock.step(2)
        cycles += 2
        commits += 1
      }
    }
  }
}
