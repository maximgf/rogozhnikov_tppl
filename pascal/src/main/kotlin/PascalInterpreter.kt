import java.lang.Exception

enum class TokenType {
    INTEGER, PLUS, MINUS, MUL, DIV, LPAREN, RPAREN,
    BEGIN, END, DOT, SEMI, ASSIGN, ID, EOF
}

data class Token(val type: TokenType, val value: String? = null)

class Lexer(private val text: String) {
    private var pos = 0
    private var currentChar: Char? = if (text.isNotEmpty()) text[0] else null

    private fun advance() {
        pos++
        currentChar = if (pos < text.length) text[pos] else null
    }

    private fun skipWhitespace() {
        while (currentChar != null && currentChar!!.isWhitespace()) {
            advance()
        }
    }

    private fun integer(): String {
        val result = StringBuilder()
        while (currentChar != null && currentChar!!.isDigit()) {
            result.append(currentChar)
            advance()
        }
        return result.toString()
    }

    private fun id(): Token {
        val result = StringBuilder()
        while (currentChar != null && (currentChar!!.isLetterOrDigit() || currentChar == '_')) {
            result.append(currentChar)
            advance()
        }
        val tokenValue = result.toString()
        return when (tokenValue.uppercase()) {
            "BEGIN" -> Token(TokenType.BEGIN, tokenValue)
            "END" -> Token(TokenType.END, tokenValue)
            else -> Token(TokenType.ID, tokenValue)
        }
    }

    fun getNextToken(): Token {
        while (currentChar != null) {
            if (currentChar!!.isWhitespace()) {
                skipWhitespace()
                continue
            }

            if (currentChar!!.isDigit()) {
                return Token(TokenType.INTEGER, integer())
            }

            if (currentChar!!.isLetter()) {
                return id()
            }

            if (currentChar == ':' && (pos + 1 < text.length && text[pos + 1] == '=')) {
                advance()
                advance()
                return Token(TokenType.ASSIGN, ":=")
            }

            when (currentChar) {
                '+' -> { advance(); return Token(TokenType.PLUS, "+") }
                '-' -> { advance(); return Token(TokenType.MINUS, "-") }
                '*' -> { advance(); return Token(TokenType.MUL, "*") }
                '/' -> { advance(); return Token(TokenType.DIV, "/") }
                '(' -> { advance(); return Token(TokenType.LPAREN, "(") }
                ')' -> { advance(); return Token(TokenType.RPAREN, ")") }
                ';' -> { advance(); return Token(TokenType.SEMI, ";") }
                '.' -> { advance(); return Token(TokenType.DOT, ".") }
                else -> throw Exception("Unknown character: $currentChar")
            }
        }
        return Token(TokenType.EOF, null)
    }
}

abstract class AST
data class BinOp(val left: AST, val op: Token, val right: AST) : AST()
data class UnaryOp(val op: Token, val expr: AST) : AST()
data class Num(val token: Token) : AST()
data class Compound(val children: List<AST>) : AST()
data class Assign(val left: Var, val op: Token, val right: AST) : AST()
data class Var(val token: Token) : AST()
class NoOp : AST()

class Parser(private val lexer: Lexer) {
    private var currentToken: Token = lexer.getNextToken()

    private fun eat(type: TokenType) {
        if (currentToken.type == type) {
            currentToken = lexer.getNextToken()
        } else {
            throw Exception("Invalid syntax: expected $type but found ${currentToken.type}")
        }
    }

    private fun factor(): AST {
        val token = currentToken
        return when (token.type) {
            TokenType.PLUS -> {
                eat(TokenType.PLUS)
                UnaryOp(token, factor())
            }
            TokenType.MINUS -> {
                eat(TokenType.MINUS)
                UnaryOp(token, factor())
            }
            TokenType.INTEGER -> {
                eat(TokenType.INTEGER)
                Num(token)
            }
            TokenType.LPAREN -> {
                eat(TokenType.LPAREN)
                val node = expr()
                eat(TokenType.RPAREN)
                node
            }
            TokenType.ID -> variable()
            else -> throw Exception("Unexpected token in factor: $token")
        }
    }

    private fun term(): AST {
        var node = factor()
        while (currentToken.type == TokenType.MUL || currentToken.type == TokenType.DIV) {
            val token = currentToken
            eat(token.type)
            node = BinOp(node, token, factor())
        }
        return node
    }

    private fun expr(): AST {
        var node = term()
        while (currentToken.type == TokenType.PLUS || currentToken.type == TokenType.MINUS) {
            val token = currentToken
            eat(token.type)
            node = BinOp(node, token, term())
        }
        return node
    }

    private fun variable(): AST {
        val node = Var(currentToken)
        eat(TokenType.ID)
        return node
    }

    private fun assignment(): AST {
        val left = variable() as Var
        val token = currentToken
        eat(TokenType.ASSIGN)
        val right = expr()
        return Assign(left, token, right)
    }

    private fun statement(): AST {
        return when (currentToken.type) {
            TokenType.BEGIN -> compoundStatement()
            TokenType.ID -> assignment()
            else -> empty()
        }
    }

    private fun statementList(): List<AST> {
        val results = mutableListOf<AST>()
        results.add(statement())

        while (currentToken.type == TokenType.SEMI) {
            eat(TokenType.SEMI)
            results.add(statement())
        }
        return results
    }

    private fun compoundStatement(): AST {
        eat(TokenType.BEGIN)
        val nodes = statementList()
        eat(TokenType.END)
        return Compound(nodes)
    }

    private fun empty(): AST {
        return NoOp()
    }

    fun parse(): AST {
        val node = compoundStatement()
        eat(TokenType.DOT)
        return node
    }
}

class Interpreter(private val parser: Parser) {
    val globalScope = mutableMapOf<String, Int>()

    fun interpret(): Map<String, Int> {
        val tree = parser.parse()
        visit(tree)
        return globalScope
    }

    private fun visit(node: AST): Int {
        return when (node) {
            is BinOp -> visitBinOp(node)
            is UnaryOp -> visitUnaryOp(node)
            is Num -> node.token.value!!.toInt()
            is Compound -> visitCompound(node)
            is Assign -> visitAssign(node)
            is Var -> visitVar(node)
            is NoOp -> 0
            else -> throw Exception("No visit method for node: $node")
        }
    }

    private fun visitCompound(node: Compound): Int {
        for (child in node.children) {
            visit(child)
        }
        return 0
    }

    private fun visitAssign(node: Assign): Int {
        val varName = node.left.token.value!!
        val value = visit(node.right)
        globalScope[varName] = value
        return value
    }

    private fun visitVar(node: Var): Int {
        val varName = node.token.value!!
        return globalScope[varName] ?: throw Exception("Variable not found: $varName")
    }

    private fun visitBinOp(node: BinOp): Int {
        val left = visit(node.left)
        val right = visit(node.right)
        return when (node.op.type) {
            TokenType.PLUS -> left + right
            TokenType.MINUS -> left - right
            TokenType.MUL -> left * right
            TokenType.DIV -> left / right
            else -> throw Exception("Unknown operator")
        }
    }

    private fun visitUnaryOp(node: UnaryOp): Int {
        val expr = visit(node.expr)
        return when (node.op.type) {
            TokenType.PLUS -> +expr
            TokenType.MINUS -> -expr
            else -> throw Exception("Unknown unary operator")
        }
    }
}

fun runInterpreter(code: String): Map<String, Int> {
    return try {
        val lexer = Lexer(code)
        val parser = Parser(lexer)
        val interpreter = Interpreter(parser)
        interpreter.interpret()
    } catch (e: Exception) {
        println("Error interpreting code: ${e.message}")
        emptyMap()
    }
}