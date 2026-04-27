package com.studymentor.chess

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class GameActivity : AppCompatActivity() {

    private lateinit var chessBoard: GridLayout
    private lateinit var tvWhiteStatus: TextView
    private lateinit var tvBlackStatus: TextView
    private lateinit var tvGameStatus: TextView
    private lateinit var tvCapturedWhiteCount: TextView
    private lateinit var tvCapturedBlackCount: TextView
    private lateinit var whitePlayer: androidx.cardview.widget.CardView
    private lateinit var blackPlayer: androidx.cardview.widget.CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNewGame: Button
    private lateinit var btnResign: Button
    private lateinit var btnDraw: Button
    private lateinit var btnMenu: Button
    private lateinit var toolbar: Toolbar

    private val cells = Array(8) { arrayOfNulls<FrameLayout>(8) }
    private var selectedRow = -1
    private var selectedCol = -1

    private lateinit var game: ChessGame
    private lateinit var bot: ChessBot
    private var gameMode = "twoPlayers"
    private var isBotThinking = false
    private var drawOffered = false
    private var vibrationEnabled = true
    private var showHints = true
    private var soundEnabled = true
    private var playerColor = "white"

    // Bluetooth мультиплеер
    private var isBluetoothGame = false
    private var isBluetoothHost = false
    private var bluetoothCallback: BluetoothCallback? = null

    private lateinit var prefs: SharedPreferences
    private var wins = 0
    private var losses = 0
    private var draws = 0

    private val capturedWhite = mutableListOf<String>()
    private val capturedBlack = mutableListOf<String>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val botExecutor = Executors.newSingleThreadExecutor()

    // ==================== Bluetooth КОМПАНЬОН ====================
    companion object {
        private var gameInstance: GameActivity? = null
        private var pendingCallback: BluetoothCallback? = null

        fun receiveMove(moveString: String) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                gameInstance?.onReceiveMove(moveString)
            }
        }

        fun setBluetoothCallback(callback: BluetoothCallback?) {
            pendingCallback = callback
            gameInstance?.bluetoothCallback = callback
        }
    }

    interface BluetoothCallback {
        fun sendMessage(message: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameInstance = this
        if (pendingCallback != null) {
            bluetoothCallback = pendingCallback
        }

        gameMode = intent.getStringExtra("game_mode") ?: "twoPlayers"
        val difficulty = intent.getStringExtra("difficulty") ?: "beginner"

        // Bluetooth параметрҳо
        isBluetoothGame = gameMode == "bluetooth"
        isBluetoothHost = intent.getBooleanExtra("is_bluetooth_host", false)

        game = ChessGame()
        bot = ChessBot(difficulty)

        prefs = getSharedPreferences("chess_stats", MODE_PRIVATE)
        loadStats()

        initViews()
        setupBoard()
        resetGame()
        setupClickListeners()

        applySettings()
        applyPlayerColorSetting()

        updateUI()

        // Если это Bluetooth игра и игрок не хост - ждем ход
        if (isBluetoothGame) {
            val role = if (isBluetoothHost) "Шумо бо Сафедҳо" else "Шумо бо Сиёҳҳо"
            Toast.makeText(this, "$role. Бозӣ оғоз шуд!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        botExecutor.shutdownNow()
        gameInstance = null
    }

    private fun initViews() {
        chessBoard = findViewById(R.id.chessBoard)
        tvWhiteStatus = findViewById(R.id.tvWhiteStatus)
        tvBlackStatus = findViewById(R.id.tvBlackStatus)
        tvGameStatus = findViewById(R.id.tvGameStatus)
        tvCapturedWhiteCount = findViewById(R.id.tvCapturedWhiteCount)
        tvCapturedBlackCount = findViewById(R.id.tvCapturedBlackCount)
        whitePlayer = findViewById(R.id.whitePlayer)
        blackPlayer = findViewById(R.id.blackPlayer)
        progressBar = findViewById(R.id.progressBar)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnResign = findViewById(R.id.btnResign)
        btnDraw = findViewById(R.id.btnDraw)
        btnMenu = findViewById(R.id.btnMenu)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { showExitDialog() }
    }

    private fun applySettings() {
        val settingsPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        vibrationEnabled = settingsPrefs.getBoolean("vibration", true)
        showHints = settingsPrefs.getBoolean("show_hints", true)
        soundEnabled = settingsPrefs.getBoolean("sound_effects", true)

        val lightColor = settingsPrefs.getString("board_light_color", "#F0D9B5") ?: "#F0D9B5"
        val darkColor = settingsPrefs.getString("board_dark_color", "#B58863") ?: "#B58863"
        updateBoardColors(lightColor, darkColor)

        val boardStyle = settingsPrefs.getString("board_style", "classic") ?: "classic"
        applyBoardStyle(boardStyle)

        val whitePieceColor = settingsPrefs.getString("white_piece_color", "#FFFFFF") ?: "#FFFFFF"
        val blackPieceColor = settingsPrefs.getString("black_piece_color", "#000000") ?: "#000000"
        updatePieceColors(whitePieceColor, blackPieceColor)

        playerColor = settingsPrefs.getString("player_color", "white") ?: "white"
    }

    private fun applyPlayerColorSetting() {
        if (gameMode == "bot" && playerColor == "black") {
            mainHandler.postDelayed({
                if (!game.isCheckmate && !game.isStalemate && !isBotThinking) {
                    makeBotMove()
                }
            }, 500)
        }
    }

    private fun updateBoardColors(lightColor: String, darkColor: String) {
        try {
            val light = Color.parseColor(lightColor)
            val dark = Color.parseColor(darkColor)
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val isLight = (row + col) % 2 == 0
                    cells[row][col]?.setBackgroundColor(if (isLight) light else dark)
                }
            }
        } catch (e: Exception) { }
    }

    private fun applyBoardStyle(style: String) {
        val lightColor = when(style) {
            "wood" -> "#DEB887"
            "marble" -> "#F5F5F5"
            "dark" -> "#2C3E50"
            else -> "#F0D9B5"
        }
        val darkColor = when(style) {
            "wood" -> "#8B4513"
            "marble" -> "#808080"
            "dark" -> "#1A252F"
            else -> "#B58863"
        }
        updateBoardColors(lightColor, darkColor)
    }

    private fun updatePieceColors(whiteColor: String, blackColor: String) {
        try {
            val white = Color.parseColor(whiteColor)
            val black = Color.parseColor(blackColor)
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val pieceText = cells[row][col]?.findViewById<TextView>(R.id.tvPiece) ?: continue
                    if (game.board[row][col]?.startsWith("white") == true) {
                        pieceText.setTextColor(white)
                    } else if (game.board[row][col]?.startsWith("black") == true) {
                        pieceText.setTextColor(black)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun vibrate() {
        if (!vibrationEnabled) return
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(50)
            }
        } catch (e: Exception) { }
    }

    private fun setupBoard() {
        chessBoard.removeAllViews()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val cellView = layoutInflater.inflate(R.layout.cell_view, chessBoard, false) as FrameLayout
                cellView.layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; height = 0
                    columnSpec = GridLayout.spec(col, 1f)
                    rowSpec = GridLayout.spec(row, 1f)
                }
                val isLight = (row + col) % 2 == 0
                cellView.setBackgroundColor(
                    ContextCompat.getColor(this, if (isLight) R.color.board_light else R.color.board_dark)
                )
                cellView.tag = Pair(row, col)
                cellView.setOnClickListener { cellClickListener(it) }
                chessBoard.addView(cellView)
                cells[row][col] = cellView
            }
        }
    }

    private fun cellClickListener(view: View) {
        if (isBotThinking || game.isCheckmate || game.isStalemate) return
        if (game.pendingPromotion != null) return

        val (row, col) = view.tag as Pair<Int, Int>

        // Bluetooth бозӣ - навбати кадом бозигар?
        if (isBluetoothGame) {
            val isMyTurn = if (isBluetoothHost) {
                game.currentPlayer == "white"
            } else {
                game.currentPlayer == "black"
            }
            if (!isMyTurn) {
                Toast.makeText(this, "Навбати рақиб", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (gameMode == "bot") {
            val isUserTurn = if (playerColor == "white") {
                game.currentPlayer == "white"
            } else {
                game.currentPlayer == "black"
            }

            if (!isUserTurn) {
                Toast.makeText(this, "Сейчас ход бота", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (selectedRow == -1) {
            val piece = game.board[row][col]
            if (piece != null && piece.startsWith(game.currentPlayer)) {
                selectCell(row, col)
            }
        } else {
            if (game.isValidMove(selectedRow, selectedCol, row, col)) {
                makeMove(selectedRow, selectedCol, row, col)
            } else {
                clearHighlights()
                selectedRow = -1; selectedCol = -1
            }
        }
    }

    private fun makeMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val capturedPiece = game.board[toRow][toCol]
        if (capturedPiece != null) {
            if (game.currentPlayer == "white") capturedBlack.add(capturedPiece)
            else capturedWhite.add(capturedPiece)
            updateCapturedDisplay()
            try {
                cells[toRow][toCol]?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
            } catch (e: Exception) { }
        }

        vibrate()

        val isPromotion = game.isPawnPromotion(fromRow, fromCol, toRow, toCol)
        if (isPromotion) {
            game.moveWithPromotion(fromRow, fromCol, toRow, toCol)
            updateBoardDisplay()
            clearHighlights(); selectedRow = -1; selectedCol = -1
            showPromotionDialog(toRow, toCol, game.promotionColor)
        } else {
            game.movePiece(fromRow, fromCol, toRow, toCol)
            updateBoardDisplay()
            clearHighlights(); selectedRow = -1; selectedCol = -1
            updateUI()

            // Отправка хода через Bluetooth
            if (isBluetoothGame && bluetoothCallback != null) {
                val moveString = "$fromRow,$fromCol,$toRow,$toCol"
                bluetoothCallback?.sendMessage("MOVE:$moveString")
            }

            if (gameMode == "bot" && !game.isCheckmate && !game.isStalemate && !isBotThinking) {
                makeBotMove()
            }
        }
    }

    // ==================== Bluetooth ФУНКЦИЯҲО ====================

    private fun onReceiveMove(moveString: String) {
        if (moveString.startsWith("MOVE:")) {
            val parts = moveString.substring(5).split(",")
            if (parts.size == 4) {
                val fromRow = parts[0].toInt()
                val fromCol = parts[1].toInt()
                val toRow = parts[2].toInt()
                val toCol = parts[3].toInt()
                runOnUiThread {
                    makeOpponentMove(fromRow, fromCol, toRow, toCol)
                }
            }
        } else if (moveString == "RESIGN") {
            runOnUiThread {
                draws++
                saveStats()
                showGameEndDialog("Рақиб таслим шуд. Шумо ғалаба кардед!")
            }
        } else if (moveString == "DRAW_OFFER") {
            runOnUiThread {
                showBluetoothDrawOfferDialog()
            }
        } else if (moveString == "DRAW_ACCEPT") {
            runOnUiThread {
                draws++
                saveStats()
                showGameEndDialog("Баробарӣ!")
            }
        } else if (moveString == "DISCONNECTED") {
            runOnUiThread {
                Toast.makeText(this, "Алоқа бо рақиб канда шуд", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun makeOpponentMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        if (game.isValidMove(fromRow, fromCol, toRow, toCol)) {
            val capturedPiece = game.board[toRow][toCol]
            if (capturedPiece != null) {
                if (game.currentPlayer == "white") capturedBlack.add(capturedPiece)
                else capturedWhite.add(capturedPiece)
                updateCapturedDisplay()
            }

            game.movePiece(fromRow, fromCol, toRow, toCol)
            updateBoardDisplay()
            clearHighlights()
            selectedRow = -1
            selectedCol = -1
            updateUI()
            vibrate()

            if (game.isCheckmate) {
                wins++
                saveStats()
                showGameEndDialog("Шумо ғалаба кардед! Шоҳу мат!")
            } else if (game.isStalemate) {
                draws++
                saveStats()
                showGameEndDialog("Пат! Баробарӣ!")
            } else if (game.isCheck) {
                Toast.makeText(this, "⚡ Шоҳ!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendBluetoothResign() {
        bluetoothCallback?.sendMessage("RESIGN")
        showGameEndDialog("Шумо таслим шудед!")
    }

    private fun sendBluetoothDrawOffer() {
        bluetoothCallback?.sendMessage("DRAW_OFFER")
        Toast.makeText(this, "Пешниҳоди баробарӣ фиристода шуд", Toast.LENGTH_SHORT).show()
    }

    private fun sendBluetoothDrawAccept() {
        bluetoothCallback?.sendMessage("DRAW_ACCEPT")
    }

    private fun showBluetoothDrawOfferDialog() {
        AlertDialog.Builder(this)
            .setTitle("Пешниҳоди баробарӣ")
            .setMessage("Рақиб баробарӣ пешниҳод мекунад. Қабул мекунед?")
            .setPositiveButton("Қабул") { _, _ ->
                draws++
                saveStats()
                sendBluetoothDrawAccept()
                showGameEndDialog("Баробарӣ!")
            }
            .setNegativeButton("Рад") { _, _ -> }
            .setCancelable(false)
            .show()
    }

    private fun makeBotMove() {
        isBotThinking = true
        progressBar.visibility = View.VISIBLE
        tvGameStatus.text = "🤖 Бот думает..."

        val gameCopy = cloneGameForBot(game)

        botExecutor.execute {
            val move = bot.getBestMove(gameCopy)

            mainHandler.post {
                if (!isDestroyed && !isFinishing) {
                    applyBotMove(move)
                }
            }
        }
    }

    private fun applyBotMove(move: Move?) {
        isBotThinking = false
        progressBar.visibility = View.GONE

        if (move == null) {
            tvGameStatus.text = ""
            return
        }

        val capturedPiece = game.board[move.to.first][move.to.second]
        if (capturedPiece != null) {
            if (playerColor == "white") {
                capturedWhite.add(capturedPiece)
            } else {
                capturedBlack.add(capturedPiece)
            }
            updateCapturedDisplay()
        }

        vibrate()

        val isPromotion = game.isPawnPromotion(
            move.from.first, move.from.second, move.to.first, move.to.second
        )

        if (isPromotion) {
            game.moveWithPromotion(move.from.first, move.from.second, move.to.first, move.to.second)
            game.promotePawn(move.to.first, move.to.second, "black_queen")
            game.currentPlayer = if (playerColor == "white") "white" else "black"
            game.updateGameState()
        } else {
            game.movePiece(move.from.first, move.from.second, move.to.first, move.to.second)
        }

        updateBoardDisplay()
        updateUI()

        when {
            game.isCheckmate -> {
                losses++
                saveStats()
                showGameEndDialog("Бот победил! Шах и мат!")
            }
            game.isStalemate -> {
                draws++
                saveStats()
                showGameEndDialog("Пат! Ничья!")
            }
            game.isCheck -> Toast.makeText(this, "⚡ Шах!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cloneGameForBot(src: ChessGame): ChessGame {
        val dst = ChessGame()
        for (i in 0 until 8) for (j in 0 until 8) dst.board[i][j] = src.board[i][j]
        dst.currentPlayer = src.currentPlayer
        dst.whiteKingPos = src.whiteKingPos
        dst.blackKingPos = src.blackKingPos
        dst.isCheck = src.isCheck
        dst.isCheckmate = src.isCheckmate
        dst.isStalemate = src.isStalemate
        dst.pendingPromotion = src.pendingPromotion
        dst.promotionColor = src.promotionColor
        dst.lastMove = src.lastMove
        return dst
    }

    private fun showPromotionDialog(row: Int, col: Int, color: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_promotion, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun promote(pieceName: String) {
            val newPiece = "${color}_$pieceName"
            game.promotePawn(row, col, newPiece)
            game.currentPlayer = if (color == "white") "black" else "white"
            game.updateGameState()
            updateBoardDisplay()
            dialog.dismiss()
            if (gameMode == "bot" && !game.isCheckmate && !game.isStalemate && !isBotThinking) {
                makeBotMove()
            }
        }

        dialogView.findViewById<Button>(R.id.btnQueen).setOnClickListener { promote("queen") }
        dialogView.findViewById<Button>(R.id.btnRook).setOnClickListener { promote("rook") }
        dialogView.findViewById<Button>(R.id.btnBishop).setOnClickListener { promote("bishop") }
        dialogView.findViewById<Button>(R.id.btnKnight).setOnClickListener { promote("knight") }
        dialog.show()
    }

    private fun updateCapturedDisplay() {
        tvCapturedWhiteCount.text = capturedWhite.size.toString()
        tvCapturedBlackCount.text = capturedBlack.size.toString()
    }

    private fun selectCell(row: Int, col: Int) {
        clearHighlights()
        selectedRow = row; selectedCol = col
        highlightCell(row, col, R.color.selected_cell)
        if (showHints) {
            showPossibleMoves(row, col)
        }
        game.lastMove?.let {
            highlightCell(it.first.first, it.first.second, R.color.last_move)
            highlightCell(it.second.first, it.second.second, R.color.last_move)
        }
    }

    private fun highlightCell(row: Int, col: Int, colorRes: Int) {
        cells[row][col]?.setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    private fun clearHighlights() {
        for (r in 0 until 8) for (c in 0 until 8) {
            val isLight = (r + c) % 2 == 0
            cells[r][c]?.setBackgroundColor(
                ContextCompat.getColor(this, if (isLight) R.color.board_light else R.color.board_dark)
            )
        }
        if (game.isCheck) {
            val kingPos = if (game.currentPlayer == "white") game.whiteKingPos else game.blackKingPos
            highlightCell(kingPos.first, kingPos.second, R.color.check)
        }
    }

    private fun showPossibleMoves(row: Int, col: Int) {
        for (r in 0 until 8) for (c in 0 until 8) {
            if (game.isValidMove(row, col, r, c)) {
                cells[r][c]?.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        if (game.board[r][c] != null) R.color.possible_capture else R.color.possible_move
                    )
                )
            }
        }
    }

    private fun resetGame() {
        isBotThinking = false
        game.resetGame()
        selectedRow = -1; selectedCol = -1
        capturedWhite.clear(); capturedBlack.clear()
        updateCapturedDisplay()
        clearHighlights()
        updateBoardDisplay()
        updateUI()
        drawOffered = false
    }

    private fun updateBoardDisplay() {
        val settingsPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        for (row in 0 until 8) for (col in 0 until 8) {
            val pieceText = cells[row][col]?.findViewById<TextView>(R.id.tvPiece) ?: continue
            pieceText.text = when (game.board[row][col]) {
                "white_king" -> "♔"; "white_queen" -> "♕"
                "white_rook" -> "♖"; "white_bishop" -> "♗"
                "white_knight" -> "♘"; "white_pawn" -> "♙"
                "black_king" -> "♚"; "black_queen" -> "♛"
                "black_rook" -> "♜"; "black_bishop" -> "♝"
                "black_knight" -> "♞"; "black_pawn" -> "♟"
                else -> ""
            }

            if (game.board[row][col]?.startsWith("white") == true) {
                val whiteColor = settingsPrefs.getString("white_piece_color", "#FFFFFF") ?: "#FFFFFF"
                pieceText.setTextColor(Color.parseColor(whiteColor))
            } else if (game.board[row][col]?.startsWith("black") == true) {
                val blackColor = settingsPrefs.getString("black_piece_color", "#000000") ?: "#000000"
                pieceText.setTextColor(Color.parseColor(blackColor))
            } else {
                pieceText.setTextColor(Color.BLACK)
            }
        }
        clearHighlights()
    }

    private fun updateUI() {
        if (game.currentPlayer == "white") {
            tvWhiteStatus.visibility = View.VISIBLE
            tvBlackStatus.visibility = View.GONE
            whitePlayer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.accent_light))
            blackPlayer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        } else {
            tvWhiteStatus.visibility = View.GONE
            tvBlackStatus.visibility = View.VISIBLE
            whitePlayer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
            blackPlayer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.accent_light))
        }

        when {
            game.isCheckmate -> {
                val winner = if (game.currentPlayer == "white") "ЧЕРНЫЕ" else "БЕЛЫЕ"
                tvGameStatus.text = "🏆 $winner ПОБЕДИЛИ!"
                tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.victory))
                if (gameMode == "twoPlayers") {
                    if (winner == "БЕЛЫЕ") wins++ else losses++
                    saveStats()
                }
                mainHandler.postDelayed({ showGameEndDialog("$winner победили! Шах и мат!") }, 500)
            }
            game.isStalemate -> {
                tvGameStatus.text = "🤝 НИЧЬЯ!"
                tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
                if (gameMode == "twoPlayers") { draws++; saveStats() }
                mainHandler.postDelayed({ showGameEndDialog("Пат! Ничья!") }, 500)
            }
            game.isCheck -> {
                val who = if (game.currentPlayer == "white") "Белые" else "Черные"
                tvGameStatus.text = "⚡ ШАХ! $who под ударом"
                tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.check))
            }
            else -> {
                val who = if (game.currentPlayer == "white") "Белые" else "Черные"
                tvGameStatus.text = "♟ Ход: $who"
                tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }

    private fun showGameEndDialog(message: String) {
        if (isDestroyed || isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("Игра окончена")
            .setMessage(message)
            .setPositiveButton("Новая игра") { _, _ -> resetGame() }
            .setNegativeButton("Статистика") { _, _ -> showStatsDialog() }
            .setNeutralButton("В меню") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showStatsDialog() {
        val winRate = if (wins + losses > 0) wins * 100 / (wins + losses) else 0
        AlertDialog.Builder(this)
            .setTitle("Статистика")
            .setMessage("Победы: $wins\nПоражения: $losses\nНичьи: $draws\nПроцент побед: $winRate%")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun loadStats() {
        wins = prefs.getInt("wins", 0)
        losses = prefs.getInt("losses", 0)
        draws = prefs.getInt("draws", 0)
    }

    private fun saveStats() {
        prefs.edit().putInt("wins", wins).putInt("losses", losses).putInt("draws", draws).apply()
    }

    private fun offerDraw() {
        if (drawOffered) return

        if (isBluetoothGame && bluetoothCallback != null) {
            sendBluetoothDrawOffer()
            drawOffered = true
            return
        }

        if (gameMode == "twoPlayers") {
            drawOffered = true
            AlertDialog.Builder(this)
                .setTitle("Предложение ничьей")
                .setMessage("Противник предлагает ничью. Принять?")
                .setPositiveButton("Принять") { _, _ ->
                    draws++; saveStats(); showGameEndDialog("Ничья!")
                }
                .setNegativeButton("Отклонить") { _, _ ->
                    drawOffered = false
                    Toast.makeText(this, "Предложение отклонено", Toast.LENGTH_SHORT).show()
                }
                .setCancelable(false).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Ничья")
                .setMessage("Закончить игру вничью?")
                .setPositiveButton("Да") { _, _ -> draws++; saveStats(); showGameEndDialog("Ничья!") }
                .setNegativeButton("Нет") { _, _ -> }
                .show()
        }
    }

    private fun resign() {
        if (isBluetoothGame && bluetoothCallback != null) {
            sendBluetoothResign()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Сдаться")
            .setMessage("Вы уверены?")
            .setPositiveButton("Да") { _, _ ->
                val winner = if (game.currentPlayer == "white") "Черные" else "Белые"
                if (gameMode == "twoPlayers") {
                    if (winner == "Белые") wins++ else losses++
                    saveStats()
                }
                showGameEndDialog("$winner победили! Противник сдался.")
            }
            .setNegativeButton("Нет") { _, _ -> }
            .show()
    }

    private fun showExitDialog() {
        if (!game.isCheckmate && !game.isStalemate) {
            AlertDialog.Builder(this)
                .setTitle("Выход из игры")
                .setMessage("Игра не сохранена. Выйти?")
                .setPositiveButton("Выйти") { _, _ -> finish() }
                .setNegativeButton("Отмена") { _, _ -> }
                .show()
        } else finish()
    }

    private fun setupClickListeners() {
        btnNewGame.setOnClickListener { resetGame() }
        btnResign.setOnClickListener { resign() }
        btnDraw.setOnClickListener { offerDraw() }
        btnMenu.setOnClickListener { showExitDialog() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { showExitDialog() }
}