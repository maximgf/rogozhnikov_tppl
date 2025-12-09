package org.example
import runInterpreter

fun main() {
    // Test Case 1
    val code1 = """
        BEGIN
        END.
    """.trimIndent()
    println("Test 1 Result: ${runInterpreter(code1)}")

    // Test Case 2
    val code2 = """
        BEGIN
            x:= 2 + 3 * (2 + 3);
            y:= 2 / 2 - 2 + 3 * ((1 + 1) + (1 + 1));
        END.
    """.trimIndent()
    println("Test 2 Result: ${runInterpreter(code2)}")

    // Test Case 3
    val code3 = """
        BEGIN
            y: = 2;
            BEGIN
                a := 3;
                a := a;
                b := 10 + a + 10 * y / 4;
                c := a - b
            END;
            x := 11;
        END.
    """.trimIndent()
    println("Test 3 Result: ${runInterpreter(code3)}")
}