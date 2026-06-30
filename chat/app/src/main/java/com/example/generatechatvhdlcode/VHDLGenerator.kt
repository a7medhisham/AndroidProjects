package com.example.generatechatvhdlcode

import kotlin.math.ceil
import kotlin.math.log2
//Singleton & Utility & Stateless
object VHDLGenerator {

    fun generate(
        query: String,
        format: String,
        entity: String,
        arch: String,
        hasEnable: Boolean = false,
        hasReset: Boolean = false
    ): String {
        return when {
            query.contains("DEMUX", ignoreCase = true)      -> generateDeMUX(query, format, entity, arch, hasEnable, hasReset)
            query.contains("MUX", ignoreCase = true)        -> generateMUX(query, format, entity, arch, hasEnable, hasReset)
            query.contains("DECODER", ignoreCase = true)    -> generateDecoder(query, format, entity, arch, hasEnable)
            query.contains("ENCODER", ignoreCase = true)    -> generateEncoder(query, format, entity, arch, hasEnable)
            query.contains("COMPARATOR", ignoreCase = true) -> generateComparator(query, format, entity, arch)
            query.contains("SRAM", ignoreCase = true)       -> generateSRAM(query, format, entity, arch, hasEnable, hasReset)
            query.contains("SIPO", ignoreCase = true)       -> generateSIPO(query, format, entity, arch, hasEnable, hasReset)
            query.contains("PISO", ignoreCase = true)       -> generatePISO(query, format, entity, arch, hasEnable, hasReset)
            query.contains("PIPO", ignoreCase = true)       -> generatePIPO(query, format, entity, arch, hasEnable, hasReset)
            else -> "-- Component not recognized.\n-- Supported: MUX, DeMUX, Decoder, Encoder, Comparator, SRAM, SIPO, PISO, PIPO"
        }
    }

    private fun extractParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        Regex("""(\w+)=(\d+)""").findAll(query).forEach {
            params[it.groupValues[1]] = it.groupValues[2] //key and value
        }
        return params
    }

    private fun generateMUX(
        query: String,
        fmt: String,
        e: String,
        a: String,
        hasEnable: Boolean,
        hasReset: Boolean
    ): String {

        val p = extractParams(query) // paramters
        val n = p["inputs"]?.toIntOrNull() ?: 4 // input
        val bitWidth = p["width"]?.toIntOrNull() ?: 1 // bit width
        val selBits = ceil(log2(n.toDouble())).toInt() //sel 

        val ports = (0 until n).joinToString(";\n           ") {
            if (bitWidth == 1)
                "IP$it : in STD_LOGIC"
            else
                "IP$it : in STD_LOGIC_VECTOR(${bitWidth - 1} downto 0)"
        }

        val selRange =
            if (selBits > 1) "STD_LOGIC_VECTOR(${selBits - 1} downto 0)"
            else "STD_LOGIC"

        val type =
            if (bitWidth == 1) "STD_LOGIC"
            else "STD_LOGIC_VECTOR(${bitWidth - 1} downto 0)"

        val outPort = "Bitout : out $type"

        fun cases(assign: (Int) -> String) =
            (0 until n).joinToString("\n            ") {
                val bin = it.toString(2).padStart(selBits, '0')
                "when \"$bin\" => ${assign(it)}"
            }

        val enablePort = if (hasEnable) "\n           enable : in STD_LOGIC;" else ""
        val resetPort = if (hasReset) "\n           reset  : in STD_LOGIC;" else ""

        val enableOpen = if (hasEnable) "if enable = '1' then\n                " else ""
        val enableClose = if (hasEnable) "\n                end if;" else ""

        val resetVal = if (bitWidth == 1) "'0'" else "(others => '0')"

        //STANDARD
        val std = """
    process(all)
    begin
        if ${if (hasReset) "reset = '1'" else "false"} then
            Bitout <= $resetVal;
        else
            $enableOpen
                case Sel is
                    ${cases { "Bitout <= IP$it;" }}
                    when others => Bitout <= IP0;
                end case;
            $enableClose
        end if;
    end process;
    """.trimIndent()

        //FUNCTION
        val funInputs = (0 until n).joinToString("; ") {
            if (bitWidth == 1)
                "I$it: STD_LOGIC"
            else
                "I$it: STD_LOGIC_VECTOR(${bitWidth - 1} downto 0)"
        }

        val funBody = """
    function mux_func($funInputs; S: $selRange)
        return $type is
        variable result : $type;
    begin
        case S is
            ${cases { "result := I$it;" }}
            when others => result := I0;
        end case;
        return result;
    end function;

begin
    process(all)
    begin
        if ${if (hasReset) "reset = '1'" else "false"} then
            Bitout <= $resetVal;
        else
            $enableOpen
                Bitout <= mux_func(${(0 until n).joinToString(", ") { "IP$it" }}, Sel);
            $enableClose
        end if;
    end process;
    """.trimIndent()

        //PROCEDURE
        val procInputs = (0 until n).joinToString("; ") {
            if (bitWidth == 1)
                "I$it: in STD_LOGIC"
            else
                "I$it: in STD_LOGIC_VECTOR(${bitWidth - 1} downto 0)"
        }

        val procBody = """
    procedure mux_proc($procInputs;
                       S: in $selRange;
                       signal Y: out $type) is
    begin
        case S is
            ${cases { "Y <= I$it;" }}
            when others => Y <= I0;
        end case;
    end procedure;

    signal tmp : $type;

begin
    process(all)
    begin
        if ${if (hasReset) "reset = '1'" else "false"} then
            tmp <= $resetVal;
        else
            $enableOpen
                mux_proc(${(0 until n).joinToString(", ") { "IP$it" }}, Sel, tmp);
            $enableClose
        end if;
    end process;

    Bitout <= tmp;
    """.trimIndent()

        val arch = when (fmt) {
            "function" -> funBody
            "procedure" -> procBody
            else -> std
        }

        return """
-- MUX Clean
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity $e is
    Port (
           $ports;
           Sel : in $selRange;$enablePort$resetPort
           $outPort
    );
end $e;

architecture $a of $e is
$arch
end $a;
""".trimIndent()
    }

    private fun generateDeMUX(
        query: String,
        fmt: String,
        e: String,
        a: String,
        hasEnable: Boolean,
        hasReset: Boolean
    ): String {

        val p = extractParams(query)
        val n = p["outputs"]?.toIntOrNull() ?: 4
        val bitWidth = p["width"]?.toIntOrNull() ?: 1

        val selBits = ceil(log2(n.toDouble())).toInt()
        val selRange = "STD_LOGIC_VECTOR(${selBits - 1} downto 0)"

        val type =
            if (bitWidth == 1) "STD_LOGIC"
            else "STD_LOGIC_VECTOR(${bitWidth - 1} downto 0)"

        val inPort = "Input : in $type"

        val outPorts = (0 until n).joinToString(";\n           ") {
            "Out$it : out $type"
        }

        val zeros = if (bitWidth == 1) "'0'" else "(others => '0')"

        val enablePort = if (hasEnable) "\n           enable : in STD_LOGIC;" else ""
        val resetPort = if (hasReset) "\n           reset  : in STD_LOGIC;" else ""

        val enableOpen = if (hasEnable) "if enable = '1' then\n                " else ""
        val enableClose = if (hasEnable) "\n                end if;" else ""

        fun cases(): String =
            (0 until n).joinToString("\n            ") { i ->
                val bin = i.toString(2).padStart(selBits, '0')
                val assigns = (0 until n).joinToString("\n                ") {
                    if (it == i) "Out$it <= Input;" else "Out$it <= $zeros;"
                }
                "when \"$bin\" =>\n                $assigns"
            }

        //STANDARD
        val std = """
    process(all)
    begin
        if ${if (hasReset) "reset = '1'" else "false"} then
            ${(0 until n).joinToString("\n            ") { "Out$it <= $zeros;" }}
        else
            $enableOpen
                case Sel is
                    ${cases()}
                    when others =>
                        ${(0 until n).joinToString("\n                        ") { "Out$it <= $zeros;" }}
                end case;
            $enableClose
        end if;
    end process;
    """.trimIndent()

        //FUNCTION
        val funBody = """
    function demux_func(I: $type; S: $selRange)
        return STD_LOGIC_VECTOR(${n * bitWidth - 1} downto 0) is
        variable result : STD_LOGIC_VECTOR(${n * bitWidth - 1} downto 0) := (others => '0');
    begin
        result(to_integer(unsigned(S)) * $bitWidth + ${bitWidth - 1} downto
               to_integer(unsigned(S)) * $bitWidth) := I;
        return result;
    end function;

    signal tmp : STD_LOGIC_VECTOR(${n * bitWidth - 1} downto 0);

begin
    tmp <= demux_func(Input, Sel);

    ${(0 until n).joinToString("\n    ") {
            "Out$it <= tmp(${it * bitWidth + bitWidth - 1} downto ${it * bitWidth});"
        }}
    """.trimIndent()

        //PROCEDURE
        val procBody = """
    procedure demux_proc(I: in $type;
                         S: in $selRange;
                         signal Y: out STD_LOGIC_VECTOR(${n * bitWidth - 1} downto 0)) is
    begin
        Y <= (others => '0');
        Y(to_integer(unsigned(S)) * $bitWidth + ${bitWidth - 1} downto
          to_integer(unsigned(S)) * $bitWidth) <= I;
    end procedure;

    signal tmp : STD_LOGIC_VECTOR(${n * bitWidth - 1} downto 0);

begin
    process(all)
    begin
        demux_proc(Input, Sel, tmp);
    end process;

    ${(0 until n).joinToString("\n    ") {
            "Out$it <= tmp(${it * bitWidth + bitWidth - 1} downto ${it * bitWidth});"
        }}
    """.trimIndent()

        val arch = when (fmt) {
            "function" -> funBody
            "procedure" -> procBody
            else -> std
        }

        return """
-- DeMUX Clean
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity $e is
    Port (
           $inPort;
           $outPorts;
           Sel : in $selRange;$enablePort$resetPort
    );
end $e;

architecture $a of $e is
$arch
end $a;
""".trimIndent()
    }

    private fun generateDecoder(query: String, fmt: String, e: String, a: String, hasEnable: Boolean): String {
        val p = extractParams(query)
        val inBits  = p["bits"]?.toIntOrNull() ?: 2
        val outBits = 1 shl inBits

        val enablePort = if (hasEnable) "\n           enable : in STD_LOGIC;" else ""
        val enableCond = if (hasEnable) "if enable = '1' then\n            " else ""
        val enableEnd  = if (hasEnable) "\n        end if;" else ""

        fun cases(assign: (String) -> String): String =
            (0 until outBits).joinToString("\n            ") {
                val bin = it.toString(2).padStart(inBits, '0')
                val out = (1 shl it).toString(2).padStart(outBits, '0')
                "when \"$bin\" => ${assign("\"$out\"")}"
            }
        //STANDARD
        val stdBody = """
    process(decoder_in${if (hasEnable) ", enable" else ""})
    begin
        decoder_out <= (others => '0');
        $enableCond
            case decoder_in is
                ${cases { v -> "decoder_out <= $v;" }}
                when others => decoder_out <= (others => '0');
            end case;$enableEnd
    end process;
    """.trimIndent()
        //FUNCTION
        val funBody = """
    function decoder_func(inp: STD_LOGIC_VECTOR(${inBits - 1} downto 0))
        return STD_LOGIC_VECTOR is
    begin
        case inp is
            ${cases { v -> "return $v;" }}
            when others => return (others => '0');
        end case;
    end function;

begin
    process(decoder_in)
    begin
        decoder_out <= decoder_func(decoder_in); -- Calling function
    end process;
    """.trimIndent()
        //PROCEDURE
        val procBody = """
    procedure decoder_proc(
        signal inp  : in  STD_LOGIC_VECTOR(${inBits - 1} downto 0);
        signal outt : out STD_LOGIC_VECTOR(${outBits - 1} downto 0)
    ) is
    begin
        case inp is
            ${cases { v -> "outt <= $v;" }}
            when others => outt <= (others => '0');
        end case;
    end procedure;

begin
    process(decoder_in)
    begin
        decoder_proc(decoder_in, decoder_out); -- Calling procedure
    end process;
    """.trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nbegin\n$stdBody\nend $a;"
        }

        return """
-- Decoder ${inBits}-to-$outBits | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity $e is
    Port (
           decoder_in  : in  STD_LOGIC_VECTOR(${inBits - 1} downto 0);$enablePort
           decoder_out : out STD_LOGIC_VECTOR(${outBits - 1} downto 0)
    );
end $e;

$archBody
""".trimIndent()
    }

    private fun generateEncoder(query: String, fmt: String, e: String, a: String, hasEnable: Boolean): String {
        val p = extractParams(query)
        val inBits  = p["bits"]?.toIntOrNull() ?: 16
        val outBits = ceil(log2(inBits.toDouble())).toInt()

        val enablePort  = if (hasEnable) "enable : in STD_LOGIC;\n           " else ""
        val enableOpen  = if (hasEnable) "if enable = '1' then\n            " else ""
        val enableClose = if (hasEnable) "\n        end if;" else ""

        fun hexCases(assign: (String) -> String): String =
            (0 until inBits).joinToString("\n            ") {
                val inHex  = (1L shl it).toString(16).uppercase().padStart(inBits / 4, '0')
                val outBin = it.toString(2).padStart(outBits, '0')
                "when X\"$inHex\" => ${assign("\"$outBin\"")}"
            }

        //STANDARD
        val stdBody = """
process(encoder_in${if (hasEnable) ", enable" else ""})
begin
    encoder_out <= (others => '0');
    $enableOpen
        case encoder_in is
            ${hexCases { v -> "encoder_out <= $v;" }}
            when others => encoder_out <= (others => '0');
        end case;$enableClose
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function encoder_func(${if (hasEnable) "en: STD_LOGIC; " else ""}D: STD_LOGIC_VECTOR(${inBits - 1} downto 0))
    return STD_LOGIC_VECTOR is
    variable result : STD_LOGIC_VECTOR(${outBits - 1} downto 0);
begin
    result := (others => '0');
    ${if (hasEnable) "if en = '1' then\n        " else ""}case D is
        ${hexCases { v -> "result := $v;" }}
        when others => result := (others => '0');
    end case;${if (hasEnable) "\n    end if;" else ""}
    return result;
end function;

begin
process(encoder_in${if (hasEnable) ", enable" else ""})
begin
    $enableOpen
        encoder_out <= encoder_func(${if (hasEnable) "enable, " else ""}encoder_in);
    $enableClose
end process;
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure encoder_proc(${if (hasEnable) "en: in STD_LOGIC; " else ""}D: in STD_LOGIC_VECTOR(${inBits - 1} downto 0);
                       Y: out STD_LOGIC_VECTOR(${outBits - 1} downto 0)) is
begin
    Y := (others => '0');
    ${if (hasEnable) "if en = '1' then\n        " else ""}case D is
        ${hexCases { v -> "Y := $v;" }}
        when others => Y := (others => '0');
    end case;${if (hasEnable) "\n    end if;" else ""}
end procedure;

begin
process(encoder_in${if (hasEnable) ", enable" else ""})
    variable v : STD_LOGIC_VECTOR(${outBits - 1} downto 0);
begin
    $enableOpen
        encoder_proc(${if (hasEnable) "enable, " else ""}encoder_in, v);
        encoder_out <= v;
    $enableClose
end process;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nbegin\n$stdBody\nend $a;"
        }

        return """
-- Encoder ${inBits}-to-$outBits${if (hasEnable) " with enable" else ""} | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity $e is
    Port ( ${enablePort}encoder_in  : in  STD_LOGIC_VECTOR(${inBits - 1} downto 0);
           encoder_out : out STD_LOGIC_VECTOR(${outBits - 1} downto 0));
end $e;

$archBody
""".trimIndent()
    }

    private fun generateComparator(query: String, fmt: String, e: String, a: String): String {
        val p = extractParams(query)
        val bits = p["bits"]?.toIntOrNull() ?: 8

        // STANDARD
        val stdBody = """
process(Bus_1, Bus_2)
begin
    if (Bus_1 > Bus_2) then
        Less    <= '0';
        Equal   <= '0';
        Greater <= '1';
    elsif (Bus_1 < Bus_2) then
        Less    <= '1';
        Equal   <= '0';
        Greater <= '0';
    else
        Less    <= '0';
        Equal   <= '1';
        Greater <= '0';
    end if;
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function compare_func(A, B: STD_LOGIC_VECTOR(${bits - 1} downto 0))
    return STD_LOGIC_VECTOR is
    variable result : STD_LOGIC_VECTOR(2 downto 0);
begin
    if (unsigned(A) > unsigned(B)) then
        result := "001";
    elsif (unsigned(A) < unsigned(B)) then
        result := "100";
    else
        result := "010";
    end if;
    return result;
end function;

signal tmp : STD_LOGIC_VECTOR(2 downto 0);

begin
process(Bus_1, Bus_2)
begin
    tmp <= compare_func(Bus_1, Bus_2);
end process;

Greater <= tmp(0);
Equal   <= tmp(1);
Less    <= tmp(2);
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure compare_proc(A, B: in STD_LOGIC_VECTOR(${bits - 1} downto 0);
                       L, Eq, G: out STD_LOGIC) is
begin
    if (unsigned(A) > unsigned(B)) then
        G := '1'; Eq := '0'; L := '0';
    elsif (unsigned(A) < unsigned(B)) then
        G := '0'; Eq := '0'; L := '1';
    else
        G := '0'; Eq := '1'; L := '0';
    end if;
end procedure;

begin
process(Bus_1, Bus_2)
    variable vL, vEq, vG : STD_LOGIC;
begin
    compare_proc(Bus_1, Bus_2, vL, vEq, vG);
    Less    <= vL;
    Equal   <= vEq;
    Greater <= vG;
end process;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nbegin\n$stdBody\nend $a;"
        }

        return """
-- Comparator ${bits}-bit | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity $e is
    Port ( Bus_1, Bus_2 : in  STD_LOGIC_VECTOR(${bits - 1} downto 0);
           Less, Equal, Greater : out STD_LOGIC);
end $e;

$archBody
""".trimIndent()
    }

    private fun generateSRAM(
        query: String,
        fmt: String,
        e: String,
        a: String,
        hasEnable: Boolean,
        hasReset: Boolean
    ): String {

        val p = extractParams(query)
        val addrBits = p["addr"]?.toIntOrNull() ?: 10
        val dataBits = p["data"]?.toIntOrNull() ?: 8
        val memSize = 1 shl addrBits

        val enablePort = if (hasEnable) "enable : in STD_LOGIC;\n           " else ""
        val resetPort  = if (hasReset)  "reset  : in STD_LOGIC;\n           " else ""

        val enableOpen  = if (hasEnable) "if enable = '1' then\n                " else ""
        val enableClose = if (hasEnable) "\n            end if;" else ""

        val resetOpen = if (hasReset) "if reset = '1' then\n            RAM1 <= (others => (others => '0'));\n        else\n            " else ""
        val resetClose = if (hasReset) "\n        end if;" else ""

        val entityBlock = """
-- SRAM ${memSize}x${dataBits} | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity $e is
    Port (
           data_in     : in  STD_LOGIC_VECTOR(${dataBits - 1} downto 0);
           Address_Bus : in  STD_LOGIC_VECTOR(${addrBits - 1} downto 0);
           Clock, WR   : in  STD_LOGIC;
           ${enablePort}${resetPort}data_out    : out STD_LOGIC_VECTOR(${dataBits - 1} downto 0)
    );
end $e;
""".trimIndent()

        //STANDARD
        val stdBody = """
process(Clock${if (hasReset) ", reset" else ""})
begin
    $resetOpen
    if rising_edge(Clock) then
        $enableOpen
        if WR = '1' then
            RAM1(to_integer(unsigned(Address_Bus))) <= data_in;
        else
            data_out <= RAM1(to_integer(unsigned(Address_Bus)));
        end if;$enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function ram_func(mem: ram_type;
                  addr: STD_LOGIC_VECTOR(${addrBits - 1} downto 0);
                  din: STD_LOGIC_VECTOR(${dataBits - 1} downto 0);
                  wr: STD_LOGIC)
    return ram_type is
    variable temp : ram_type := mem;
begin
    if wr = '1' then
        temp(to_integer(unsigned(addr))) := din;
    end if;
    return temp;
end function;

begin
process(Clock${if (hasReset) ", reset" else ""})
begin
    $resetOpen
    if rising_edge(Clock) then
        $enableOpen
        RAM1 <= ram_func(RAM1, Address_Bus, data_in, WR);
        data_out <= RAM1(to_integer(unsigned(Address_Bus)));
        $enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure ram_proc(
    signal mem : inout ram_type;
    addr       : in STD_LOGIC_VECTOR(${addrBits - 1} downto 0);
    din        : in STD_LOGIC_VECTOR(${dataBits - 1} downto 0);
    wr         : in STD_LOGIC
) is
begin
    if wr = '1' then
        mem(to_integer(unsigned(addr))) <= din;
    end if;
end procedure;

begin
process(Clock${if (hasReset) ", reset" else ""})
begin
    $resetOpen
    if rising_edge(Clock) then
        $enableOpen
        ram_proc(RAM1, Address_Bus, data_in, WR);
        data_out <= RAM1(to_integer(unsigned(Address_Bus)));
        $enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n    type ram_type is array(0 to ${memSize - 1}) of STD_LOGIC_VECTOR(${dataBits - 1} downto 0);\n    signal RAM1 : ram_type := (others => (others => '0'));\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n    type ram_type is array(0 to ${memSize - 1}) of STD_LOGIC_VECTOR(${dataBits - 1} downto 0);\n    signal RAM1 : ram_type := (others => (others => '0'));\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\n    type ram_type is array(0 to ${memSize - 1}) of STD_LOGIC_VECTOR(${dataBits - 1} downto 0);\n    signal RAM1 : ram_type := (others => (others => '0'));\nbegin\n$stdBody\nend $a;"
        }

        return "$entityBlock\n\n$archBody"
    }

    private fun generateSIPO(query: String, fmt: String, e: String, a: String, hasEnable: Boolean, hasReset: Boolean): String {

        val p = extractParams(query)
        val bits = p["bits"]?.toIntOrNull() ?: 8
        val dir  = if (query.contains("left", true)) "left" else "right"

        val shiftExpr = if (dir == "right")
            "reg <= ser_in & reg(${bits - 1} downto 1);"
        else
            "reg <= reg(${bits - 2} downto 0) & ser_in;"

        val enableOpen  = if (hasEnable) "if enable = '1' then\n                " else ""
        val enableClose = if (hasEnable) "\n            end if;" else ""

        val resetOpen = if (hasReset) "if Reset = '1' then\n            reg <= (others => '0');\n        else\n            " else ""
        val resetClose = if (hasReset) "\n        end if;" else ""

        //STANDARD
        val stdBody = """
process(clk${if (hasReset) ", Reset" else ""})
begin
    $resetOpen
    if rising_edge(clk) then
        $enableOpen
        $shiftExpr
        $enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function sipo_func(D: STD_LOGIC;
                   reg: STD_LOGIC_VECTOR(${bits - 1} downto 0))
    return STD_LOGIC_VECTOR is
begin
    ${if (dir == "right") "return D & reg(${bits - 1} downto 1);" else "return reg(${bits - 2} downto 0) & D;"}
end function;

begin
process(clk${if (hasReset) ", Reset" else ""})
begin
    $resetOpen
    if rising_edge(clk) then
        $enableOpen
        reg <= sipo_func(ser_in, reg);
        $enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure sipo_proc(D: in STD_LOGIC;
                    signal reg: inout STD_LOGIC_VECTOR(${bits - 1} downto 0)) is
begin
    ${if (dir == "right") "reg <= D & reg(${bits - 1} downto 1);" else "reg <= reg(${bits - 2} downto 0) & D;"}
end procedure;

begin
process(clk${if (hasReset) ", Reset" else ""})
begin
    $resetOpen
    if rising_edge(clk) then
        $enableOpen
        sipo_proc(ser_in, reg);
        $enableClose
    end if;
    $resetClose
end process;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\nsignal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\nsignal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nsignal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');\nbegin\n$stdBody\nend $a;"
        }

        return """
-- SIPO ${bits}-bit shift-$dir | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity $e is
    Port ( ser_in, clk${if (hasReset) ", Reset" else ""}${if (hasEnable) ", enable" else ""} : in STD_LOGIC;
           Dout : out STD_LOGIC_VECTOR(${bits - 1} downto 0));
end $e;

$archBody
""".trimIndent()
    }

    private fun generatePISO(
        query: String,
        fmt: String,
        e: String,
        a: String,
        hasEnable: Boolean,
        hasReset: Boolean
    ): String {

        val p = extractParams(query)
        val bits = p["bits"]?.toIntOrNull() ?: 8

        val enableLine = if (hasEnable)
            "\n           enable  : in STD_LOGIC;"
        else ""

        //STANDARD
        val stdBody = """
process(CIK, Rst, mode, DIN)
    variable var1 : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');
begin
    if Rst = '1' then
        ser_out <= '0';
        var1 := (others => '0');

    elsif rising_edge(CIK) then
        if enable = '1' then
            case mode is
                when "00" =>
                    var1 := DIN;

                when "01" =>
                    ser_out <= var1(0);
                    var1 := '0' & var1(${bits - 1} downto 1);

                when "10" =>
                    ser_out <= var1(${bits - 1});
                    var1 := var1(${bits - 2} downto 0) & '0';

                when others => null;
            end case;
        end if;
    end if;
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function piso_func(
    D    : STD_LOGIC_VECTOR(${bits - 1} downto 0);
    mode : STD_LOGIC_VECTOR(1 downto 0);
    reg  : STD_LOGIC_VECTOR(${bits - 1} downto 0)
) return STD_LOGIC_VECTOR is

    variable new_reg : STD_LOGIC_VECTOR(${bits - 1} downto 0);

begin
    case mode is
        when "00" =>
            new_reg := D;

        when "01" =>
            new_reg := '0' & reg(${bits - 1} downto 1);

        when "10" =>
            new_reg := reg(${bits - 2} downto 0) & '0';

        when others =>
            new_reg := reg;
    end case;

    return new_reg;
end function;

signal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');

begin
process(CIK, Rst)
begin
    if Rst = '1' then
        reg <= (others => '0');
        ser_out <= '0';

    elsif rising_edge(CIK) then
        reg <= piso_func(DIN, mode, reg);

        if mode = "01" then
            ser_out <= reg(0);
        elsif mode = "10" then
            ser_out <= reg(${bits - 1});
        else
            ser_out <= '0';
        end if;
    end if;
end process;
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure piso_proc(
    D     : in STD_LOGIC_VECTOR(${bits - 1} downto 0);
    mode  : in STD_LOGIC_VECTOR(1 downto 0);
    signal reg   : inout STD_LOGIC_VECTOR(${bits - 1} downto 0);
    signal s_out : out STD_LOGIC
) is
begin
    case mode is
        when "00" =>
            reg <= D;
            s_out <= '0';

        when "01" =>
            s_out <= reg(0);
            reg <= '0' & reg(${bits - 1} downto 1);

        when "10" =>
            s_out <= reg(${bits - 1});
            reg <= reg(${bits - 2} downto 0) & '0';

        when others =>
            s_out <= '0';
    end case;
end procedure;

signal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');

begin
process(CIK, Rst)
begin
    if Rst = '1' then
        reg <= (others => '0');
        ser_out <= '0';

    elsif rising_edge(CIK) then
        piso_proc(DIN, mode, reg, ser_out);
    end if;
end process;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nbegin\n$stdBody\nend $a;"
        }

        return """
-- PISO ${bits}-bit | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity $e is
    Port (
        DIN     : in  STD_LOGIC_VECTOR(${bits - 1} downto 0);
        CIK, Rst: in  STD_LOGIC;
        mode    : in  STD_LOGIC_VECTOR(1 downto 0);$enableLine
        ser_out : out STD_LOGIC
    );
end $e;

$archBody
""".trimIndent()
    }
    private fun generatePIPO(
        query: String,
        fmt: String,
        e: String,
        a: String,
        hasEnable: Boolean,
        hasReset: Boolean
    ): String {

        val p = extractParams(query)
        val bits = p["bits"]?.toIntOrNull() ?: 8

        //STANDARD
        val stdBody = """
process(CLK, Reset)
begin
    if Reset = '1' then
        Dout <= (others => '0');

    elsif rising_edge(CLK) then
        if Enable = '1' then
            Dout <= DIN;
        end if;
    end if;
end process;
""".trimIndent()

        //FUNCTION
        val funBody = """
function pipo_func(
    D    : STD_LOGIC_VECTOR(${bits - 1} downto 0);
    en   : STD_LOGIC;
    rst  : STD_LOGIC;
    reg  : STD_LOGIC_VECTOR(${bits - 1} downto 0)
) return STD_LOGIC_VECTOR is
begin
    if rst = '1' then
        return (others => '0');

    elsif en = '1' then
        return D;

    else
        return reg;
    end if;
end function;

signal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');

begin
process(CLK, Reset)
begin
    if Reset = '1' then
        reg <= (others => '0');

    elsif rising_edge(CLK) then
        reg <= pipo_func(DIN, Enable, Reset, reg);
    end if;
end process;

Dout <= reg;
""".trimIndent()

        //PROCEDURE
        val procBody = """
procedure pipo_proc(
    D    : in STD_LOGIC_VECTOR(${bits - 1} downto 0);
    en   : in STD_LOGIC;
    rst  : in STD_LOGIC;
    signal reg : inout STD_LOGIC_VECTOR(${bits - 1} downto 0)
) is
begin
    if rst = '1' then
        reg <= (others => '0');

    elsif en = '1' then
        reg <= D;
    end if;
end procedure;

signal reg : STD_LOGIC_VECTOR(${bits - 1} downto 0) := (others => '0');

begin
process(CLK, Reset)
begin
    if Reset = '1' then
        reg <= (others => '0');

    elsif rising_edge(CLK) then
        pipo_proc(DIN, Enable, Reset, reg);
    end if;
end process;

Dout <= reg;
""".trimIndent()

        val archBody = when (fmt) {
            "function"  -> "architecture $a of $e is\n$funBody\nend $a;"
            "procedure" -> "architecture $a of $e is\n$procBody\nend $a;"
            else        -> "architecture $a of $e is\nbegin\n$stdBody\nend $a;"
        }

        return """
-- PIPO ${bits}-bit | $fmt format
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity $e is
    Port (
        DIN    : in  STD_LOGIC_VECTOR(${bits - 1} downto 0);
        CLK    : in  STD_LOGIC;
        Enable : in  STD_LOGIC;
        Reset  : in  STD_LOGIC;
        Dout   : out STD_LOGIC_VECTOR(${bits - 1} downto 0)
    );
end $e;

$archBody
""".trimIndent()
    }
}