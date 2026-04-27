package com.studymentor.chess

data class ChessGame(
    var board: Array<Array<String?>> = Array(8) { arrayOfNulls(8) },
    var currentPlayer: String = "white",
    var whiteKingPos: Pair<Int, Int> = Pair(7, 4),
    var blackKingPos: Pair<Int, Int> = Pair(0, 4),
    var lastMove: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    var isCheck: Boolean = false,
    var isCheckmate: Boolean = false,
    var isStalemate: Boolean = false,
    var pendingPromotion: Pair<Int, Int>? = null,
    var promotionColor: String = "white"
) {

    fun isValidMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val piece = board[fromRow][fromCol] ?: return false
        if (fromRow == toRow && fromCol == toCol) return false
        if (board[toRow][toCol]?.startsWith(currentPlayer) == true) return false

        val isValid = when {
            piece.contains("pawn") -> isValidPawnMove(fromRow, fromCol, toRow, toCol)
            piece.contains("rook") -> isValidRookMove(fromRow, fromCol, toRow, toCol)
            piece.contains("knight") -> isValidKnightMove(fromRow, fromCol, toRow, toCol)
            piece.contains("bishop") -> isValidBishopMove(fromRow, fromCol, toRow, toCol)
            piece.contains("queen") -> isValidQueenMove(fromRow, fromCol, toRow, toCol)
            piece.contains("king") -> isValidKingMove(fromRow, fromCol, toRow, toCol)
            else -> false
        }

        return isValid && !wouldMoveLeaveKingInCheck(fromRow, fromCol, toRow, toCol)
    }

    private fun wouldMoveLeaveKingInCheck(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val targetPiece = board[toRow][toCol]
        val movingPiece = board[fromRow][fromCol]

        board[toRow][toCol] = movingPiece
        board[fromRow][fromCol] = null

        val oldKingPos = if (currentPlayer == "white") whiteKingPos else blackKingPos
        if (movingPiece?.contains("king") == true) {
            if (currentPlayer == "white") whiteKingPos = Pair(toRow, toCol)
            else blackKingPos = Pair(toRow, toCol)
        }

        val kingInCheck = isKingInCheck(currentPlayer)

        board[fromRow][fromCol] = movingPiece
        board[toRow][toCol] = targetPiece

        if (movingPiece?.contains("king") == true) {
            if (currentPlayer == "white") whiteKingPos = oldKingPos
            else blackKingPos = oldKingPos
        }

        return kingInCheck
    }

    fun isKingInCheck(player: String): Boolean {
        val kingPos = if (player == "white") whiteKingPos else blackKingPos
        val opponent = if (player == "white") "black" else "white"

        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val piece = board[r][c]
                if (piece != null && piece.startsWith(opponent)) {
                    if (canPieceAttackSquare(r, c, kingPos.first, kingPos.second)) return true
                }
            }
        }
        return false
    }

    private fun canPieceAttackSquare(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val piece = board[fromRow][fromCol] ?: return false
        return when {
            piece.contains("pawn") -> isValidPawnAttack(fromRow, fromCol, toRow, toCol)
            piece.contains("knight") -> isValidKnightMove(fromRow, fromCol, toRow, toCol)
            piece.contains("bishop") -> isValidBishopMove(fromRow, fromCol, toRow, toCol)
            piece.contains("rook") -> isValidRookMove(fromRow, fromCol, toRow, toCol)
            piece.contains("queen") -> isValidQueenMove(fromRow, fromCol, toRow, toCol)
            piece.contains("king") -> isValidKingMove(fromRow, fromCol, toRow, toCol)
            else -> false
        }
    }

    private fun isValidPawnAttack(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val direction = if (board[fromRow][fromCol]?.startsWith("white") == true) -1 else 1
        return Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction
    }

    fun getAllValidMoves(player: String): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val piece = board[r][c]
                if (piece != null && piece.startsWith(player)) {
                    for (tr in 0 until 8) {
                        for (tc in 0 until 8) {
                            if (isValidMove(r, c, tr, tc)) {
                                moves.add(Move(Pair(r, c), Pair(tr, tc)))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    fun hasAnyValidMove(player: String): Boolean = getAllValidMoves(player).isNotEmpty()

    fun movePiece(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val piece = board[fromRow][fromCol]
        board[toRow][toCol] = piece
        board[fromRow][fromCol] = null

        if (piece == "white_king") whiteKingPos = Pair(toRow, toCol)
        else if (piece == "black_king") blackKingPos = Pair(toRow, toCol)

        lastMove = Pair(Pair(fromRow, fromCol), Pair(toRow, toCol))
        currentPlayer = if (currentPlayer == "white") "black" else "white"
        updateGameState()
    }

    fun isPawnPromotion(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val piece = board[fromRow][fromCol] ?: return false
        if (!piece.contains("pawn")) return false
        val isWhite = piece.startsWith("white")
        return (isWhite && toRow == 0) || (!isWhite && toRow == 7)
    }

    fun promotePawn(row: Int, col: Int, newPiece: String) {
        board[row][col] = newPiece
        pendingPromotion = null
        updateGameState()
    }

    fun moveWithPromotion(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val piece = board[fromRow][fromCol]
        board[toRow][toCol] = piece
        board[fromRow][fromCol] = null

        if (piece == "white_king") whiteKingPos = Pair(toRow, toCol)
        else if (piece == "black_king") blackKingPos = Pair(toRow, toCol)

        lastMove = Pair(Pair(fromRow, fromCol), Pair(toRow, toCol))

        if (piece?.contains("pawn") == true && (toRow == 0 || toRow == 7)) {
            pendingPromotion = Pair(toRow, toCol)
            promotionColor = if (piece.startsWith("white")) "white" else "black"
        } else {
            currentPlayer = if (currentPlayer == "white") "black" else "white"
            updateGameState()
        }
    }

    fun updateGameState() {
        val opponent = currentPlayer
        isCheck = isKingInCheck(opponent)
        isCheckmate = isCheck && !hasAnyValidMove(opponent)
        isStalemate = !isCheck && !hasAnyValidMove(opponent)
    }

    fun resetGame() {
        for (i in 0 until 8) for (j in 0 until 8) board[i][j] = null

        // Белые
        board[7][0] = "white_rook"; board[7][1] = "white_knight"
        board[7][2] = "white_bishop"; board[7][3] = "white_queen"
        board[7][4] = "white_king"; board[7][5] = "white_bishop"
        board[7][6] = "white_knight"; board[7][7] = "white_rook"
        for (i in 0 until 8) board[6][i] = "white_pawn"

        // Черные
        board[0][0] = "black_rook"; board[0][1] = "black_knight"
        board[0][2] = "black_bishop"; board[0][3] = "black_queen"
        board[0][4] = "black_king"; board[0][5] = "black_bishop"
        board[0][6] = "black_knight"; board[0][7] = "black_rook"
        for (i in 0 until 8) board[1][i] = "black_pawn"

        whiteKingPos = Pair(7, 4)
        blackKingPos = Pair(0, 4)
        currentPlayer = "white"
        lastMove = null
        isCheck = false
        isCheckmate = false
        isStalemate = false
        pendingPromotion = null
        promotionColor = "white"
    }

    private fun isValidPawnMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val direction = if (board[fromRow][fromCol]?.startsWith("white") == true) -1 else 1
        val startRow = if (direction == -1) 6 else 1

        return if (fromCol == toCol) {
            if (toRow == fromRow + direction && board[toRow][toCol] == null) true
            else fromRow == startRow && toRow == fromRow + 2 * direction &&
                    board[fromRow + direction][toCol] == null && board[toRow][toCol] == null
        } else Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction &&
                board[toRow][toCol]?.startsWith(if (direction == -1) "black" else "white") == true
    }

    private fun isValidRookMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (fromRow != toRow && fromCol != toCol) return false
        val rowStep = when { toRow > fromRow -> 1; toRow < fromRow -> -1; else -> 0 }
        val colStep = when { toCol > fromCol -> 1; toCol < fromCol -> -1; else -> 0 }
        var r = fromRow + rowStep
        var c = fromCol + colStep
        while (r != toRow || c != toCol) {
            if (board[r][c] != null) return false
            r += rowStep
            c += colStep
        }
        return true
    }

    private fun isValidKnightMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val rowDiff = Math.abs(toRow - fromRow)
        val colDiff = Math.abs(toCol - fromCol)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }

    private fun isValidBishopMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol)) return false
        val rowStep = if (toRow > fromRow) 1 else -1
        val colStep = if (toCol > fromCol) 1 else -1
        var r = fromRow + rowStep
        var c = fromCol + colStep
        while (r != toRow) {
            if (board[r][c] != null) return false
            r += rowStep
            c += colStep
        }
        return true
    }

    private fun isValidQueenMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        return isValidRookMove(fromRow, fromCol, toRow, toCol) || isValidBishopMove(fromRow, fromCol, toRow, toCol)
    }

    private fun isValidKingMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1
    }
}

data class Move(val from: Pair<Int, Int>, val to: Pair<Int, Int>)