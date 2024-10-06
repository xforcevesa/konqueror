package experiments

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import experiments.regfile.Regfile

class RegfileSpec extends AnyFlatSpec with Matchers {
  "Scroller" should "scroll" in {
    simulate(new Regfile) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()
      var commits, cycles: Int = 0
      while (commits >= 100) {
        val randon_value = scala.util.Random.nextInt(1 << 32)
        val random_index = scala.util.Random.nextInt(1 << 5)
        dut.write.waddr.poke(random_index.U(5.W))
        dut.write.wdata.poke(randon_value.U(32.W))
        dut.io.we.poke(true.B)
        dut.clock.step()
        dut.io.we.poke(false.B)
        dut.clock.step()
        if (commits % 2 == 0) {
          dut.read_a.raddr.poke(random_index.U(5.W))
          dut.clock.step()
          dut.read_a.rdata.expect(randon_value.U(32.W))
          dut.clock.step()
        } else {
          dut.read_b.raddr.poke(random_index.U(5.W))
          dut.clock.step()
          dut.read_b.rdata.expect(randon_value.U(32.W))
          dut.clock.step()
        }
        commits += 1
        cycles += 4
      }
    }
  }
}
