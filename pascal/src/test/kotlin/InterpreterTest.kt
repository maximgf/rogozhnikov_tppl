import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LexerTest {

    @Test
    fun testOperatorsAndGrouping() {
        val text = "+ - * / ( ) ; ."
        val expected = listOf(
            Token(TokenType.PLUS, "+"),
            Token(TokenType.MINUS, "-"),
            Token(TokenType.MUL, "*"),
            Token(TokenType.DIV, "/"),
            Token(TokenType.LPAREN, "("),
            Token(TokenType.RPAREN, ")"),
            Token(TokenType.SEMI, ";"),
            Token(TokenType.DOT, "."),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testKeywords() {
        val text = "BEGIN END"
        val expected = listOf(
            Token(TokenType.BEGIN, "BEGIN"),
            Token(TokenType.END, "END"),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testIdentifiers() {
        val text = "x y1 var_name"
        val expected = listOf(
            Token(TokenType.ID, "x"),
            Token(TokenType.ID, "y1"),
            Token(TokenType.ID, "var_name"),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testIntegers() {
        val text = "123 0 4567"
        val expected = listOf(
            Token(TokenType.INTEGER, "123"),
            Token(TokenType.INTEGER, "0"),
            Token(TokenType.INTEGER, "4567"),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testAssignment() {
        val text = "x:=10; y:= 20"
        val expected = listOf(
            Token(TokenType.ID, "x"),
            Token(TokenType.ASSIGN, ":="),
            Token(TokenType.INTEGER, "10"),
            Token(TokenType.SEMI, ";"),
            Token(TokenType.ID, "y"),
            Token(TokenType.ASSIGN, ":="),
            Token(TokenType.INTEGER, "20"),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testWhitespaceHandling() {
        val text = " BEGIN \n x := \t 1 + 2 END ."
        val expected = listOf(
            Token(TokenType.BEGIN, "BEGIN"),
            Token(TokenType.ID, "x"),
            Token(TokenType.ASSIGN, ":="),
            Token(TokenType.INTEGER, "1"),
            Token(TokenType.PLUS, "+"),
            Token(TokenType.INTEGER, "2"),
            Token(TokenType.END, "END"),
            Token(TokenType.DOT, "."),
            Token(TokenType.EOF, null)
        )
        val lexer = Lexer(text)
        val actual = generateSequence { lexer.getNextToken() }.take(expected.size).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun testUnknownCharacterError() {
        val text = "BEGIN @ END."
        val lexer = Lexer(text)
        lexer.getNextToken()
        assertFailsWith<Exception> {
            lexer.getNextToken()
        }
    }
}

class InterpreterTest {

    private fun run(code: String): Map<String, Int> {
        val lexer = Lexer(code)
        val parser = Parser(lexer)
        val interpreter = Interpreter(parser)
        return interpreter.interpret()
    }

    @Test
    fun testEmptyProgram() {
        val code = "BEGIN END."
        assertEquals(emptyMap(), run(code))
    }

    @Test
    fun testSimpleAssignment() {
        val code = "BEGIN x := 10 END."
        assertEquals(mapOf("x" to 10), run(code))
    }

    @Test
    fun testArithmeticOperations() {
        val code = """
            BEGIN
                x:= 2 + 3 * (2 + 3);
                y:= 2 / 2 - 2 + 3 * ((1 + 1) + (1 + 1))
            END.
        """.trimIndent()
        val expected = mapOf("x" to 17, "y" to 11)
        assertEquals(expected, run(code))
    }

    @Test
    fun testIntegerDivision() {
        val code = "BEGIN z := 10 / 3; w := 1 / 2 END."
        val expected = mapOf("z" to 3, "w" to 0)
        assertEquals(expected, run(code))
    }

    @Test
    fun testNestedBlocksAndReassignment() {
        val code = """
            BEGIN
                y := 2;
                BEGIN
                    a := 3;
                    a := a;
                    b := 10 + a + 10 * y / 4;
                    c := a - b
                END;
                x := 11
            END.
        """.trimIndent()
        val expected = mapOf("y" to 2, "a" to 3, "b" to 18, "c" to -15, "x" to 11)
        assertEquals(expected, run(code))
    }

    @Test
    fun testUnaryOperators() {
        val code = "BEGIN x := -10 + +5; y := -(-2) END."
        val expected = mapOf("x" to -5, "y" to 2)
        assertEquals(expected, run(code))
    }

    @Test
    fun testVariableUsageInAssignment() {
        val code = "BEGIN a := 5; b := a + 3; c := a * b END."
        val expected = mapOf("a" to 5, "b" to 8, "c" to 40)
        assertEquals(expected, run(code))
    }

    @Test
    fun testEmptyStatements() {
        val code = "BEGIN x := 10;; END."
        val expected = mapOf("x" to 10)
        assertEquals(expected, run(code))

        val code2 = "BEGIN ;; x := 20; END."
        val expected2 = mapOf("x" to 20)
        assertEquals(expected2, run(code2))
    }

    @Test
    fun testSyntaxErrorMissingSemicolon() {
        val code = "BEGIN x := 10 y := 20 END."
        assertFailsWith<Exception> {
            run(code)
        }
    }

    @Test
    fun testSemanticErrorUnknownVariable() {
        val code = "BEGIN x := y + 1 END."
        assertFailsWith<Exception> {
            run(code)
        }
    }

    @Test
    fun testSyntaxErrorMissingDot() {
        val code = "BEGIN x := 10 END"
        assertFailsWith<Exception> {
            run(code)
        }
    }
}