package experiments

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import experiments.minicpu.MiniCPUTop
import chiseltest._
import scala.util.control.Breaks
import scala.util.Random

class MiniCPUTopSpec extends AnyFlatSpec with ChiselScalatestTester {

  val inst_array = Array(
    // addr: 0x1c000000
    0x0280000c, // addi.w   $t0,$zero,0x0      //置第1项的0
    0x0280040d, // addi.w   $t1,$zero,0x1      //置第2项的1
    0x02800017, // addi.w   $s0,$zero,0x0      //循环变量i初始化为0
    0x02800418, // addi.w   $s1,$zero,0x1      //循环的步长置为1
    0x28900004, // ld.w     $a0,$zero,1024     //读取拨码开关输入的终止值
    // loop:
    0x0010358e, // add.w    $t2,$t0,$t1        //f(i) = f(i-2) + f(i-1)
    0x028001ac, // addi.w   $t0,$t1,0x0        //记录f(i-1)
    0x028001cd, // addi.w   $t1,$t2,0x0        //记录f(i)
    0x001062f7, // add.w    $s0,$s0,$s1        //i++
    0x5ffff2e4, // bne      $s0,$a0,loop       //if i!=n, goto loop
    0x2990100e, // st.w     $t2,$zero,1028     //将f(n)的值输出到数码管上
    // end:
    0x5c000300 // bne      $s1, $zero, end    //测试完毕，进入死循环
  )

  var read_data: Int = 0
  var write_data: Int = 0

  def memoryAccess(index: Int): Int = {
    // println("memoryAccess(" + index.toHexString + ")")
    val array_index = (index - 0x1c000000) / 4
    if (array_index >= 0 && array_index < inst_array.length) {
      inst_array(array_index)
    } else {
      read_data
    }
  }

  def memoryWrite(index: Int, value: Int): Unit = {
    // println("memoryWrite(" + index.toHexString + ", " + value.toHexString + ")")
    val array_index = (index - 0x1c000000) / 4
    if (array_index >= 0 && array_index < inst_array.length) {
      inst_array(array_index) = value
    } else if (index == 1028) {
      write_data = value
    }
  }

  def memoryHandle(
      inst_we: Boolean,
      inst_addr: Int,
      inst_wdata: Int,
      data_we: Boolean,
      data_addr: Int,
      data_wdata: Int
  ): Tuple2[Int, Int] = {
    if (inst_we) {
      memoryWrite(inst_addr, inst_wdata)
    }
    if (data_we) {
      memoryWrite(data_addr, data_wdata)
    }
    Tuple2(memoryAccess(inst_addr), memoryAccess(data_addr))
  }

  def debugInstructionToString(inst: Int): String = {
    if (inst == 0) {
      "add.w"
    } else if (inst == 1) {
      "addi.w"
    } else if (inst == 2) {
      "ld.w"
    } else if (inst == 3) {
      "st.w"
    } else if (inst == 4) {
      "bne"
    } else {
      "unknown"
    }
  }

  def simulateSingleCycle(dut: MiniCPUTop): Unit = {
    val data_we = dut.io.data_sram_we.peek().litToBoolean

    val inst_addr = dut.io.inst_sram_addr.peek().litValue.toInt

    val data_addr = dut.io.data_sram_addr.peek().litValue.toInt
    val data_wdata = if (data_we) {
      dut.io.data_sram_wdata.peek().litValue.toInt
    } else {
      0
    }

    val (inst_rdata, data_rdata) = memoryHandle(
      false,
      inst_addr,
      0,
      data_we,
      data_addr,
      data_wdata,
    )
    dut.io.inst_sram_rdata.poke(inst_rdata.U(32.W))
    dut.io.data_sram_rdata.poke(data_rdata.U(32.W))

    dut.clock.step()
  }

  def fibonacci(n: Int): Int = {
    if (n <= 1) {
      n
    } else {
      fibonacci(n - 1) + fibonacci(n - 2)
    }
  }

  def simulateOneProgram(dut: MiniCPUTop): Unit = {
    var n = Random.nextInt() % 23 + 1
    if (n <= 0) {
      n = 1 - n
    }
    val res = fibonacci(n)
    read_data = n
    var cycles = 0
    val loop = new Breaks
    loop.breakable {
      while (res != write_data) {
        simulateSingleCycle(dut)
        cycles += 1
        val debug_pc = dut.io.debug_pc.peek().litValue.toInt
        val debug_inst = dut.io.debug_inst.peek().litValue.toInt
        println(
          "Cycles: " + cycles + ", PC: " + debug_pc.toHexString + ", Inst: " + debugInstructionToString(
            debug_inst
          )
        )
        if (cycles > 1000) {
          loop.break();
        }
      }
    }
    println("fibonacci(" + n + ") = " + write_data + ", res: " + res)
    write_data = 0
  }

  "MiniCPUTop" should "execute instructions correctly" in {
    test(new MiniCPUTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // dut.reset.poke(true.B)
      // dut.clock.step()
      // dut.reset.poke(false.B)
      // dut.clock.step()

      simulateOneProgram(dut)
    }
  }
}
