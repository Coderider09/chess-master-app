package com.studymentor.chess

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * КЛАСС CHESSBOT - ПРОФЕССИОНАЛЬНЫЙ ШАХМАТНЫЙ ДВИЖОК
 *
 * Этот класс реализует искусственный интеллект для игры в шахматы.
 * Использует современные техники: таблицы транспозиции, итеративное углубление,
 * альфа-бета отсечение, тихий поиск (quiescence), MVV-LVA сортировку ходов.
 *
 * @param difficulty уровень сложности: beginner, amateur, expert, master, grandmaster
 */
class ChessBot(val difficulty: String = "beginner") {

    // ====================================================================
    //  ТАБЛИЦЫ ПОЗИЦИОННОЙ ОЦЕНКИ
    //  Эти таблицы взяты из профессионального движка PeSTO
    //  Значения в сотых долях пешки (1.00 = 100)
    //  Строка 0 = верх доски (сторона чёрных), строка 7 = низ (белые)
    // ====================================================================

    /**
     * Таблица позиционной оценки пешек
     * Поощряет пешки в центре и продвинутые вперёд
     */
    private val PAWN_TABLE = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),  // ряд 8 - чёрные пешки в начале
        intArrayOf( 78,  83,  86,  73,  73,  86,  83,  78),  // ряд 7 - бонус за продвижение
        intArrayOf(  7,  29,  21,  44,  44,  21,  29,   7),  // ряд 6
        intArrayOf(-17,  16,  -2,  15,  15,  -2,  16, -17),  // ряд 5
        intArrayOf(-26,   3,  10,   9,   9,  10,   3, -26),  // ряд 4
        intArrayOf(-22,   9,   5, -11, -11,   5,   9, -22),  // ряд 3
        intArrayOf(-15,  12, -22,  -6,  -6, -22,  12, -15),  // ряд 2
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0)   // ряд 1 - белые пешки в начале
    )

    /**
     * Таблица позиционной оценки коней
     * Кони любят центр, не любят края и углы
     */
    private val KNIGHT_TABLE = arrayOf(
        intArrayOf(-66, -53, -75, -75, -75, -75, -53, -66),  // углы - очень плохо
        intArrayOf(-42, -20, -10, -10, -10, -10, -20, -42),  // края - плохо
        intArrayOf(-23,  -3,  -1,   4,   4,  -1,  -3, -23),  // центр - лучше
        intArrayOf(-29, -15, -20,  -5,  -5, -20, -15, -29),
        intArrayOf(-28, -20, -21,  -5,  -5, -21, -20, -28),
        intArrayOf(-29, -12,  -8,   2,   2,  -8, -12, -29),
        intArrayOf(-42, -20,  -9,  -1,  -1,  -9, -20, -42),
        intArrayOf(-66, -53, -75, -75, -75, -75, -53, -66)   // углы - очень плохо
    )

    /**
     * Таблица позиционной оценки слонов
     * Слоны любят центр и открытые диагонали
     */
    private val BISHOP_TABLE = arrayOf(
        intArrayOf(-59, -78, -82, -76, -76, -82, -78, -59),
        intArrayOf(-67, -26,  -9, -10, -10,  -9, -26, -67),
        intArrayOf(-58,  -9,  15,   7,   7,  15,  -9, -58),
        intArrayOf(-57,   6,  -8,  17,  17,  -8,   6, -57),
        intArrayOf(-55,   5,  11,  16,  16,  11,   5, -55),
        intArrayOf(-56,  -1,  -5,  12,  12,  -5,  -1, -56),
        intArrayOf(-55, -30,  -4, -10, -10,  -4, -30, -55),
        intArrayOf(-59, -78, -82, -76, -76, -82, -78, -59)
    )

    /**
     * Таблица позиционной оценки ладей
     * Ладьи любят открытые вертикали
     */
    private val ROOK_TABLE = arrayOf(
        intArrayOf( 35,  29,  33,   4,   4,  33,  29,  35),
        intArrayOf( 55,  29,  56,  67,  67,  56,  29,  55),
        intArrayOf( 26,  25,  23,  26,  26,  23,  25,  26),
        intArrayOf( 24,  18,  26,  24,  24,  26,  18,  24),
        intArrayOf( 17,  17,  17,  17,  17,  17,  17,  17),
        intArrayOf( -1,  -6,  13,   7,   7,  13,  -6,  -1),
        intArrayOf(-14,  -3,  -5,  -9,  -9,  -5,  -3, -14),
        intArrayOf( -9, -13,  -8, -17, -17,  -8, -13,  -9)
    )

    /**
     * Таблица позиционной оценки ферзей
     */
    private val QUEEN_TABLE = arrayOf(
        intArrayOf(-28,   0,  29,  12,  12,  29,   0, -28),
        intArrayOf(-24, -39,  -5,   1,   1,  -5, -39, -24),
        intArrayOf(-13, -17,   7,   8,   8,   7, -17, -13),
        intArrayOf(-21,   4,   5,   2,   2,   5,   4, -21),
        intArrayOf( -4,   2,   4,   5,   5,   4,   2,  -4),
        intArrayOf( -8,   6,  10,  12,  12,  10,   6,  -8),
        intArrayOf(-20,   6,   9,  14,  14,   9,   6, -20),
        intArrayOf(-28,   0,  29,  12,  12,  29,   0, -28)
    )

    /**
     * Таблица позиционной оценки короля в миттельшпиле
     * Король должен быть в безопасности (рокировка)
     */
    private val KING_MID_TABLE = arrayOf(
        intArrayOf(-65,  23,  16, -15, -15,  16,  23, -65),  // после рокировки лучше
        intArrayOf( 29,  -1, -20,  -7,  -7, -20,  -1,  29),
        intArrayOf( -9, -18,  -3, -16, -16,  -3, -18,  -9),
        intArrayOf(-17, -20, -12, -27, -27, -12, -20, -17),
        intArrayOf(-49,  -1, -27, -39, -39, -27,  -1, -49),
        intArrayOf(-14, -14, -22, -46, -46, -22, -14, -14),
        intArrayOf(  1,   7,  -8, -64, -64,  -8,   7,   1),
        intArrayOf( 33,  -3, -14, -18, -18, -14,  -3,  33)
    )

    /**
     * Таблица позиционной оценки короля в эндшпиле
     * В эндшпиле король должен идти в центр
     */
    private val KING_END_TABLE = arrayOf(
        intArrayOf(-74, -35, -18, -18, -18, -18, -35, -74),
        intArrayOf(-35,  -8,  11,  15,  15,  11,  -8, -35),
        intArrayOf(-18,  11,  24,  29,  29,  24,  11, -18),
        intArrayOf(-18,  15,  29,  42,  42,  29,  15, -18),
        intArrayOf(-18,  15,  29,  42,  42,  29,  15, -18),
        intArrayOf(-18,  11,  24,  29,  29,  24,  11, -18),
        intArrayOf(-35,  -8,  11,  15,  15,  11,  -8, -35),
        intArrayOf(-74, -35, -18, -18, -18, -18, -35, -74)
    )

    // ====================================================================
    //  ТАБЛИЦА ТРАНСПОЗИЦИЙ
    //  Кэширует уже оценённые позиции, чтобы не пересчитывать их заново
    // ====================================================================

    /**
     * Структура записи в таблице транспозиций
     * @param depth глубина поиска, на которой получена оценка
     * @param score численная оценка позиции
     * @param flag тип оценки: 0=точная, 1=нижняя граница, 2=верхняя граница
     */
    private data class TTEntry(val depth: Int, val score: Int, val flag: Int)

    /**
     * Хэш-таблица для хранения уже просчитанных позиций
     * Размер 2^16 = 65536 записей
     */
    private val transpositionTable = HashMap<Long, TTEntry>(1 shl 16)

    // ====================================================================
    //  ЗНАЧЕНИЯ ФИГУР
    //  Стандартные шахматные ценности: пешка=100, конь=320, слон=330, ладья=500, ферзь=900
    // ====================================================================

    /**
     * Возвращает материальную ценность фигуры
     * @param piece строка с названием фигуры (например "black_queen")
     * @return ценность в сотых долях пешки
     */
    private fun pieceVal(piece: String): Int = when {
        piece.contains("queen")  -> 900   // Ферзь = 9 пешек
        piece.contains("rook")   -> 500   // Ладья = 5 пешек
        piece.contains("bishop") -> 330   // Слон = 3.3 пешки
        piece.contains("knight") -> 320   // Конь = 3.2 пешки
        piece.contains("pawn")   -> 100   // Пешка = 1 пешка
        piece.contains("king")   -> 20000 // Король бесценен
        else -> 0
    }

    /**
     * Возвращает позиционную ценность фигуры на конкретной клетке
     * @param piece фигура
     * @param row строка (0-7)
     * @param col столбец (0-7)
     * @param endgame true если эндшпиль (для короля используется другая таблица)
     * @return позиционный бонус
     */
    private fun posVal(piece: String, row: Int, col: Int, endgame: Boolean): Int {
        // Для чёрных фигур таблицы читаются сверху вниз, для белых - снизу вверх
        val r = if (piece.startsWith("black")) row else 7 - row
        return when {
            piece.contains("pawn")   -> PAWN_TABLE[r][col]
            piece.contains("knight") -> KNIGHT_TABLE[r][col]
            piece.contains("bishop") -> BISHOP_TABLE[r][col]
            piece.contains("rook")   -> ROOK_TABLE[r][col]
            piece.contains("queen")  -> QUEEN_TABLE[r][col]
            piece.contains("king")   -> if (endgame) KING_END_TABLE[r][col] else KING_MID_TABLE[r][col]
            else -> 0
        }
    }

    // ====================================================================
    //  ОГРАНИЧЕНИЕ ВРЕМЕНИ
    //  Бот думает не более 4 секунд, чтобы UI не зависал
    // ====================================================================

    @Volatile private var searchStart = 0L      // Время начала поиска
    @Volatile private var abortSearch = false   // Флаг прерывания поиска
    private val TIME_LIMIT_MS = 4000L           // Лимит времени 4 секунды

    /**
     * Проверяет, не истекло ли время поиска
     * @return true если время вышло и поиск нужно прервать
     */
    private fun timeUp(): Boolean {
        if (abortSearch) return true
        val over = System.currentTimeMillis() - searchStart > TIME_LIMIT_MS
        if (over) abortSearch = true
        return over
    }

    // ====================================================================
    //  ХЭШИРОВАНИЕ ПОЗИЦИИ
    //  Простой, но эффективный хэш для таблицы транспозиций
    // ====================================================================

    /**
     * Вычисляет хэш текущей позиции на доске
     * Учитывает расположение всех фигур и очередь хода
     * @param game текущее состояние игры
     * @return 64-битный хэш
     */
    private fun hashBoard(game: ChessGame): Long {
        var h = 0L
        // Проходим по всем клеткам доски
        for (i in 0 until 8) for (j in 0 until 8) {
            val p = game.board[i][j]
            if (p != null) {
                // Смешиваем позицию, ценность фигуры и цвет
                h = h * 31 + (i * 8 + j) * pieceVal(p).toLong() +
                        if (p.startsWith("black")) 1L else 2L
            }
        }
        // Учитываем, чей сейчас ход
        return h xor (if (game.currentPlayer == "black") 0x123456789ABCDEFL else 0L)
    }

    // ====================================================================
    //  ГЛАВНЫЙ МЕТОД
    //  Вызывается из GameActivity для получения лучшего хода
    // ====================================================================

    /**
     * Главная функция получения лучшего хода для бота
     * @param game текущее состояние игры
     * @return лучший ход (Move) или null если ходов нет
     */
    fun getBestMove(game: ChessGame): Move? {
        val moves = game.getAllValidMoves("black")
        if (moves.isEmpty()) return null

        // Инициализируем таймер и очищаем таблицу транспозиций
        searchStart = System.currentTimeMillis()
        abortSearch = false
        transpositionTable.clear()

        // В зависимости от уровня сложности вызываем соответствующую стратегию
        return when (difficulty) {
            "beginner"    -> beginnerMove(game, moves)           // Уровень 1: случайные ходы + защита
            "amateur"     -> amateurMove(game, moves)            // Уровень 2: базовые взятия + безопасность
            "expert"      -> iterativeDeepening(game, moves, maxDepth = 3)  // Уровень 3: глубина 3
            "master"      -> iterativeDeepening(game, moves, maxDepth = 5)  // Уровень 4: глубина 5
            "grandmaster" -> iterativeDeepening(game, moves, maxDepth = 7)  // Уровень 5: глубина 7
            else          -> beginnerMove(game, moves)
        }
    }

    // ====================================================================
    //  УРОВЕНЬ 1 — НАЧИНАЮЩИЙ
    //  Простые ходы, базовые защиты
    // ====================================================================

    /**
     * Стратегия для уровня "Начинающий"
     * - Если шах - выбираем лучший ход
     * - Пытаемся защитить ценные фигуры
     * - Иначе случайный безопасный ход
     */
    private fun beginnerMove(game: ChessGame, moves: List<Move>): Move {
        // Если король под шахом - нужно защищаться осмысленно
        if (game.isCheck) return bestByEval(game, moves)

        // Пытаемся защитить ценные фигуры (ценнее 300)
        val save = savePiece(game, moves, minValue = 300)
        if (save != null) return save

        // Выбираем безопасный ход (где фигуру не съедят)
        val safe = moves.filter { !isLosing(game, it) }
        return if (safe.isNotEmpty()) safe.random() else moves.random()
    }

    // ====================================================================
    //  УРОВЕНЬ 2 — ЛЮБИТЕЛЬ
    //  Уже считает взятия, предпочитает центр
    // ====================================================================

    /**
     * Стратегия для уровня "Любитель"
     * - Защита ценных фигур (ценнее 100)
     * - Выгодные взятия (сортировка по ценности)
     * - Ходы в центр
     * - Безопасные ходы
     */
    private fun amateurMove(game: ChessGame, moves: List<Move>): Move {
        if (game.isCheck) return bestByEval(game, moves)

        // Защищаем ценные фигуры (ценнее 100)
        val save = savePiece(game, moves, minValue = 100)
        if (save != null) return save

        // Выгодные взятия: сортируем по ценности взятой фигуры
        val goodCaptures = moves
            .filter { game.board[it.to.first][it.to.second] != null && !isLosing(game, it) }
            .sortedByDescending { pieceVal(game.board[it.to.first][it.to.second]!!) }
        if (goodCaptures.isNotEmpty()) return goodCaptures.first()

        // Ходы в центр
        val centerMoves = moves.filter { !isLosing(game, it) && centerBonus(it) > 0 }
        if (centerMoves.isNotEmpty()) return centerMoves.random()

        // Безопасные ходы
        val safe = moves.filter { !isLosing(game, it) }
        return if (safe.isNotEmpty()) safe.random() else moves.random()
    }

    // ====================================================================
    //  ИТЕРАТИВНОЕ УГЛУБЛЕНИЕ
    //  Постепенно увеличиваем глубину поиска, сохраняем лучший ход
    // ====================================================================

    /**
     * Итеративное углубление с Aspiration Window
     * Сначала ищем на глубине 1, потом на 2, и т.д. до maxDepth
     * Это позволяет остановиться в любой момент и вернуть лучший найденный ход
     *
     * Aspiration Window: предполагаем, что оценка хода будет в узком окне
     * (предыдущая оценка ± 50). Это значительно ускоряет поиск.
     *
     * @param game текущая игра
     * @param moves список возможных ходов
     * @param maxDepth максимальная глубина поиска
     * @return лучший найденный ход
     */
    private fun iterativeDeepening(game: ChessGame, moves: List<Move>, maxDepth: Int): Move {
        var bestMove = orderMoves(game, moves).first()
        var prevScore = 0  // Оценка с предыдущей глубины

        for (depth in 1..maxDepth) {
            if (timeUp()) break  // Время вышло - прерываем

            // Aspiration window: сужаем окно поиска для ускорения
            var alpha = if (depth > 2) prevScore - 50 else Int.MIN_VALUE + 1
            var beta  = if (depth > 2) prevScore + 50 else Int.MAX_VALUE - 1

            var iterBest = bestMove
            var iterScore = Int.MIN_VALUE + 1
            val ordered = orderMoves(game, moves)

            var fullSearch = false
            // Пытаемся найти ход в узком окне
            while (true) {
                iterScore = Int.MIN_VALUE + 1
                iterBest = bestMove

                for (move in ordered) {
                    if (timeUp()) break
                    val c = cloneGame(game)
                    applyMove(c, move)
                    val score = -negamax(c, depth - 1, -beta, -alpha, false)
                    if (score > iterScore) {
                        iterScore = score
                        iterBest = move
                    }
                    alpha = max(alpha, score)
                    if (alpha >= beta) break  // Альфа-бета отсечение
                }

                if (timeUp()) break

                // Если вышли за пределы окна — делаем полный поиск с широким окном
                if (!fullSearch && (iterScore <= alpha - 50 || iterScore >= beta + 50)) {
                    alpha = Int.MIN_VALUE + 1
                    beta  = Int.MAX_VALUE - 1
                    fullSearch = true
                    continue
                }
                break
            }

            if (!timeUp()) {
                bestMove = iterBest
                prevScore = iterScore
            }
        }

        return bestMove
    }

    // ====================================================================
    //  NEGAMAX
    //  Минимакс в форме negamax — проще и быстрее
    //  Счёт всегда с точки зрения текущего игрока:
    //  положительный = хорошо для того, кто сейчас ходит
    // ====================================================================

    /**
     * Основной алгоритм поиска ходов (Negamax с альфа-бета отсечением)
     *
     * Принцип: оценка позиции с точки зрения текущего игрока.
     * Если ход хорош для чёрных, то для белых он будет плох (отрицательный).
     *
     * @param game текущее состояние игры
     * @param depth оставшаяся глубина поиска
     * @param alpha нижняя граница (лучший найденный ход)
     * @param beta верхняя граница
     * @param isBlack true если ходят чёрные (бот)
     * @return оценка позиции
     */
    private fun negamax(game: ChessGame, depth: Int, alphaIn: Int, betaIn: Int, isBlack: Boolean): Int {
        if (timeUp()) return evalForCurrent(game, isBlack)

        // Проверяем таблицу транспозиций
        val hash = hashBoard(game)
        val ttEntry = transpositionTable[hash]
        if (ttEntry != null && ttEntry.depth >= depth) {
            when (ttEntry.flag) {
                0 -> return ttEntry.score                        // Точная оценка
                1 -> if (ttEntry.score >= betaIn) return betaIn  // Нижняя граница
                2 -> if (ttEntry.score <= alphaIn) return alphaIn // Верхняя граница
            }
        }

        // Проверяем окончание игры
        if (game.isCheckmate) return -200000 + (10 - depth) * 1000  // Мат - очень плохо
        if (game.isStalemate) return 0  // Пат - ничья

        val color = if (isBlack) "black" else "white"
        val moves = game.getAllValidMoves(color)
        if (moves.isEmpty()) return evalForCurrent(game, isBlack)

        // На малой глубине используем тихий поиск
        if (depth == 0) return quiescence(game, alphaIn, betaIn, isBlack)

        var alpha = alphaIn
        val ordered = orderMoves(game, moves)
        var best = Int.MIN_VALUE + 1
        var flag = 2 // upper bound

        for ((idx, move) in ordered.withIndex()) {
            if (timeUp()) break
            val c = cloneGame(game)
            applyMove(c, move)

            // Principal Variation Search:
            // Первый ход исследуем полным окном
            // Остальные - нулевым окном для отсечения
            val score = if (idx == 0) {
                -negamax(c, depth - 1, -betaIn, -alpha, !isBlack)
            } else {
                // Нулевое окно: проверяем, лучше ли этот ход
                var s = -negamax(c, depth - 1, -alpha - 1, -alpha, !isBlack)
                // Если нашли лучший ход — перепроверяем полным окном
                if (s > alpha && s < betaIn) {
                    s = -negamax(c, depth - 1, -betaIn, -alpha, !isBlack)
                }
                s
            }

            if (score > best) {
                best = score
                flag = 0 // exact
            }
            alpha = max(alpha, score)
            if (alpha >= betaIn) {
                flag = 1 // lower bound
                break
            }
        }

        // Сохраняем в таблицу транспозиций
        if (!timeUp() && transpositionTable.size < (1 shl 17)) {
            transpositionTable[hash] = TTEntry(depth, best, flag)
        }

        return best
    }

    // ====================================================================
    //  ТИХИЙ ПОИСК (QUIESCENCE)
    //  Продолжаем поиск только взятий, пока позиция не "успокоится"
    //  Это предотвращает эффект "горизонтального" разбегания
    // ====================================================================

    /**
     * Тихий поиск (Quiescence Search)
     * Анализирует только взятия фигур, чтобы избежать эффекта "горизонтального разбегания"
     * @param game текущая игра
     * @param alpha нижняя граница
     * @param beta верхняя граница
     * @param isBlack true если ходят чёрные
     * @return оценка позиции
     */
    private fun quiescence(game: ChessGame, alphaIn: Int, betaIn: Int, isBlack: Boolean): Int {
        // Статическая оценка текущей позиции
        val standPat = evalForCurrent(game, isBlack)
        if (timeUp()) return standPat
        if (standPat >= betaIn) return betaIn  // Отсечение

        var alpha = max(alphaIn, standPat)

        val color = if (isBlack) "black" else "white"
        // Берём только взятия, сортируем по MVV-LVA
        val captures = game.getAllValidMoves(color)
            .filter { game.board[it.to.first][it.to.second] != null }
            .sortedByDescending { mvvLva(game, it) }

        for (move in captures) {
            if (timeUp()) break

            // Delta pruning: если даже со взятием не можем улучшить alpha - пропускаем
            val captureVal = pieceVal(game.board[move.to.first][move.to.second]!!)
            if (standPat + captureVal + 200 < alpha) continue

            val c = cloneGame(game)
            applyMove(c, move)
            val score = -quiescence(c, -betaIn, -alpha, !isBlack)
            if (score >= betaIn) return betaIn
            alpha = max(alpha, score)
        }

        return alpha
    }

    // ====================================================================
    //  СОРТИРОВКА ХОДОВ
    //  Чтобы сначала проверять самые перспективные ходы
    // ====================================================================

    /**
     * Сортирует ходы по перспективности:
     * 1. Мат в 1 ход (наивысший приоритет)
     * 2. Взятия (MVV-LVA - Most Valuable Victim - Least Valuable Aggressor)
     * 3. Шахи
     * 4. Ходы в центр
     * 5. Продвижение пешек
     *
     * @param game текущая игра
     * @param moves список ходов для сортировки
     * @return отсортированный список ходов
     */
    private fun orderMoves(game: ChessGame, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val victim   = game.board[move.to.first][move.to.second]
            val attacker = game.board[move.from.first][move.from.second]

            // 1. Мат в 1 — наивысший приоритет
            val c = cloneGame(game)
            applyMove(c, move)
            if (c.isCheckmate) return@sortedByDescending 1_000_000

            // 2. MVV-LVA (Most Valuable Victim - Least Valuable Aggressor)
            if (victim != null && attacker != null) {
                score += pieceVal(victim) * 10 - pieceVal(attacker)
            }

            // 3. Шах
            if (c.isCheck) score += 800

            // 4. Центр
            score += centerBonus(move)

            // 5. Продвижение пешки
            if (attacker != null && attacker.contains("pawn")) {
                score += if (attacker.startsWith("black")) move.to.first * 4
                else (7 - move.to.first) * 4
            }

            score
        }
    }

    /**
     * MVV-LVA для тихого поиска
     * Most Valuable Victim - Least Valuable Aggressor
     * @return очки ценности взятия
     */
    private fun mvvLva(game: ChessGame, move: Move): Int {
        val victim   = game.board[move.to.first][move.to.second] ?: return 0
        val attacker = game.board[move.from.first][move.from.second] ?: return 0
        return pieceVal(victim) * 10 - pieceVal(attacker)
    }

    // ====================================================================
    //  ОЦЕНКА ПОЗИЦИИ
    //  Сердце шахматного движка
    // ====================================================================

    /**
     * Оценивает позицию с точки зрения текущего игрока
     * @param game текущая игра
     * @param isBlack true если оцениваем с точки зрения чёрных
     * @return оценка (положительная = хорошо для текущего игрока)
     */
    private fun evalForCurrent(game: ChessGame, isBlack: Boolean): Int {
        val abs = evalAbsolute(game)
        return if (isBlack) abs else -abs
    }

    /**
     * Абсолютная оценка позиции
     * Положительная = хорошо для чёрных
     *
     * Учитывает:
     * - Материал (ценность фигур)
     * - Позиционные бонусы (таблицы)
     * - Безопасность королей
     * - Структуру пешек
     * - Мобильность
     * - Открытые линии для ладей
     * - Пары слонов
     */
    private fun evalAbsolute(game: ChessGame): Int {
        // Мат или пат
        if (game.isCheckmate) {
            return if (game.currentPlayer == "black") -200000 else 200000
        }
        if (game.isStalemate) return 0

        val endgame = isEndgame(game)
        var score = 0

        // 1. Материал + позиционные бонусы
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val piece = game.board[i][j] ?: continue
                val v = pieceVal(piece) + posVal(piece, i, j, endgame)
                if (piece.startsWith("black")) score += v else score -= v
            }
        }

        // 2. Безопасность королей
        score += kingSafety(game, "black")
        score -= kingSafety(game, "white")

        // 3. Пешечная структура
        score += pawnStructure(game, "black")
        score -= pawnStructure(game, "white")

        // 4. Мобильность (количество ходов)
        if (!endgame) {
            score += game.getAllValidMoves("black").size * 4
            score -= game.getAllValidMoves("white").size * 4
        }

        // 5. Пара слонов — бонус
        val blackBishops = countPiece(game, "black_bishop")
        val whiteBishops = countPiece(game, "white_bishop")
        if (blackBishops >= 2) score += 30
        if (whiteBishops >= 2) score -= 30

        // 6. Открытые линии для ладей
        score += rookOpenFile(game, "black")
        score -= rookOpenFile(game, "white")

        // 7. Шах противнику
        if (game.isCheck) {
            if (game.currentPlayer == "white") score += 40 else score -= 40
        }

        return score
    }

    // ====================================================================
    //  ВСПОМОГАТЕЛЬНЫЕ ОЦЕНОЧНЫЕ ФУНКЦИИ
    // ====================================================================

    /**
     * Определяет, наступил ли эндшпиль
     * Эндшпиль — когда на доске мало фигур
     * @return true если эндшпиль
     */
    private fun isEndgame(game: ChessGame): Boolean {
        var queens = 0; var heavies = 0
        for (i in 0 until 8) for (j in 0 until 8) {
            val p = game.board[i][j] ?: continue
            if (p.contains("queen")) queens++
            if (p.contains("rook") || p.contains("bishop") || p.contains("knight")) heavies++
        }
        return queens == 0 || (queens <= 2 && heavies <= 2)
    }

    /**
     * Считает количество фигур определённого типа на доске
     */
    private fun countPiece(game: ChessGame, pieceStr: String): Int {
        var cnt = 0
        for (i in 0 until 8) for (j in 0 until 8) if (game.board[i][j] == pieceStr) cnt++
        return cnt
    }

    /**
     * Оценивает бонус за ладьи на открытых вертикалях
     * Ладья на открытой линии (нет своих пешек) получает бонус
     */
    private fun rookOpenFile(game: ChessGame, color: String): Int {
        var bonus = 0
        val ownPawn = "${color}_pawn"
        val oppPawn = if (color == "black") "white_pawn" else "black_pawn"

        for (j in 0 until 8) {
            var hasOwnPawn = false; var hasOppPawn = false
            for (i in 0 until 8) {
                if (game.board[i][j] == ownPawn) hasOwnPawn = true
                if (game.board[i][j] == oppPawn) hasOppPawn = true
            }
            // Ладья на открытой линии
            for (i in 0 until 8) {
                val p = game.board[i][j]
                if (p != null && p.startsWith(color) && p.contains("rook")) {
                    if (!hasOwnPawn && !hasOppPawn) bonus += 20  // Полностью открытая
                    else if (!hasOwnPawn) bonus += 10           // Полуоткрытая
                }
            }
        }
        return bonus
    }

    /**
     * Оценивает безопасность короля
     * - Пешечный щит перед королём
     * - Штраф за открытого короля
     * - Бонус за рокировку
     * - Штраф за атаки вблизи короля
     */
    private fun kingSafety(game: ChessGame, color: String): Int {
        val kingPos = if (color == "black") game.blackKingPos else game.whiteKingPos
        var safety = 0
        val pawn = "${color}_pawn"
        val dir  = if (color == "black") 1 else -1
        val opp  = if (color == "black") "white" else "black"

        // Пешечный щит перед королём
        for (dc in -1..1) {
            val pr = kingPos.first + dir; val pc = kingPos.second + dc
            if (pr in 0..7 && pc in 0..7 && game.board[pr][pc] == pawn) safety += 20
            // Пешка на 2 хода вперёд — меньший бонус
            val pr2 = kingPos.first + dir * 2
            if (pr2 in 0..7 && pc in 0..7 && game.board[pr2][pc] == pawn) safety += 8
        }

        // Штраф за открытого короля (не за рокировкой)
        if (!isEndgame(game) && kingPos.second in 2..5) safety -= 30

        // Бонус за рокировку
        if (kingPos.second == 6 || kingPos.second == 2) safety += 40

        // Штраф за атаки вблизи короля
        for (dr in -2..2) for (dc in -2..2) {
            val r = kingPos.first + dr; val c = kingPos.second + dc
            if (r in 0..7 && c in 0..7) {
                for (ar in 0 until 8) for (ac in 0 until 8) {
                    val p = game.board[ar][ac]
                    if (p != null && p.startsWith(opp) && !p.contains("king") &&
                        game.isValidMove(ar, ac, r, c)) {
                        safety -= if (abs(dr) <= 1 && abs(dc) <= 1) 12 else 5
                    }
                }
            }
        }
        return safety
    }

    /**
     * Оценивает структуру пешек
     * - Штраф за сдвоенные пешки
     * - Штраф за изолированные пешки
     * - Бонус за проходные пешки
     */
    private fun pawnStructure(game: ChessGame, color: String): Int {
        var score = 0
        val pawnStr = "${color}_pawn"
        val cols = mutableListOf<Int>()

        // Собираем все столбцы, где есть пешки
        for (i in 0 until 8) for (j in 0 until 8) {
            if (game.board[i][j] == pawnStr) cols.add(j)
        }

        val cnt = cols.groupingBy { it }.eachCount()
        for ((col, count) in cnt) {
            if (count > 1) score -= 15 * (count - 1)        // Сдвоенные пешки
            if ((col - 1) !in cnt && (col + 1) !in cnt) score -= 12  // Изолированные
        }

        // Проходные пешки (нет вражеских пешек впереди)
        for (i in 0 until 8) for (j in 0 until 8) {
            if (game.board[i][j] == pawnStr && isPassedPawn(game, i, j, color)) {
                val advance = if (color == "black") i else 7 - i
                score += 20 + advance * 8  // Чем дальше, тем больше бонус
            }
        }
        return score
    }

    /**
     * Проверяет, является ли пешка проходной
     * Проходная пешка — перед которой нет вражеских пешек
     */
    private fun isPassedPawn(game: ChessGame, row: Int, col: Int, color: String): Boolean {
        val oppPawn = if (color == "black") "white_pawn" else "black_pawn"
        val range = if (color == "black") (row + 1 until 8) else (0 until row)
        for (r in range) for (dc in -1..1) {
            val c = col + dc
            if (c in 0..7 && game.board[r][c] == oppPawn) return false
        }
        return true
    }

    // ====================================================================
    //  ЗАЩИТА ВИСЯЧИХ ФИГУР
    //  Если ценная фигура под боем и её нечем защитить — уводим в безопасное место
    // ====================================================================

    /**
     * Пытается защитить ценные фигуры, которые находятся под боем
     * @param game текущая игра
     * @param moves список всех ходов
     * @param minValue минимальная ценность фигуры, которую стоит защищать
     * @return ход для защиты, или null
     */
    private fun savePiece(game: ChessGame, moves: List<Move>, minValue: Int): Move? {
        data class Threat(val row: Int, val col: Int, val value: Int)
        val threats = mutableListOf<Threat>()

        // Находим все чёрные фигуры под боем
        for (i in 0 until 8) for (j in 0 until 8) {
            val piece = game.board[i][j] ?: continue
            if (!piece.startsWith("black") || piece.contains("king")) continue
            val pv = pieceVal(piece)
            if (pv < minValue) continue

            // Ищем атакующего
            var minAttacker = Int.MAX_VALUE
            for (r in 0 until 8) for (c in 0 until 8) {
                val wp = game.board[r][c]
                if (wp != null && wp.startsWith("white") && game.isValidMove(r, c, i, j)) {
                    minAttacker = min(minAttacker, pieceVal(wp))
                }
            }
            if (minAttacker < pv) threats.add(Threat(i, j, pv))
        }

        if (threats.isEmpty()) return null
        val top = threats.maxByOrNull { it.value }!!

        // Ищем безопасный ход для этой фигуры
        val escapes = moves.filter {
            it.from.first == top.row && it.from.second == top.col && !isLosing(game, it)
        }
        if (escapes.isNotEmpty()) return escapes.maxByOrNull { evalAfterMove(game, it) }

        // Или атакуем атакующую фигуру
        for (r in 0 until 8) for (c in 0 until 8) {
            val wp = game.board[r][c]
            if (wp != null && wp.startsWith("white") && game.isValidMove(r, c, top.row, top.col)) {
                val caps = moves.filter { it.to.first == r && it.to.second == c && !isLosing(game, it) }
                if (caps.isNotEmpty()) return caps.first()
            }
        }
        return null
    }

    // ====================================================================
    //  БАЗОВЫЕ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
    // ====================================================================

    /**
     * Проверяет, является ли ход проигрышным (фигуру съедят с выгодой)
     */
    private fun isLosing(game: ChessGame, move: Move): Boolean {
        val moving = game.board[move.from.first][move.from.second] ?: return false
        val target = game.board[move.to.first][move.to.second]
        val gain = if (target != null) pieceVal(target) else 0
        val c = cloneGame(game)
        applyMove(c, move)
        for (r in 0 until 8) for (col in 0 until 8) {
            val wp = c.board[r][col]
            if (wp != null && wp.startsWith("white") && c.isValidMove(r, col, move.to.first, move.to.second)) {
                if (pieceVal(moving) - gain > 50) return true
            }
        }
        return false
    }

    /**
     * Бонус за ход в центр
     */
    private fun centerBonus(move: Move): Int {
        val r = move.to.first; val c = move.to.second
        return when {
            r in 3..4 && c in 3..4 -> 35   // Ядро центра
            r in 2..5 && c in 2..5 -> 15   // Около центра
            else -> 0
        }
    }

    /**
     * Оценивает позицию после хода
     */
    private fun evalAfterMove(game: ChessGame, move: Move): Int {
        val c = cloneGame(game); applyMove(c, move); return evalAbsolute(c)
    }

    /**
     * Выбирает лучший ход по прямой оценке
     */
    private fun bestByEval(game: ChessGame, moves: List<Move>): Move =
        moves.maxByOrNull { evalAfterMove(game, it) } ?: moves.first()

    /**
     * Применяет ход к игре (учитывает превращение пешки)
     */
    private fun applyMove(game: ChessGame, move: Move) {
        game.movePiece(move.from.first, move.from.second, move.to.first, move.to.second)
        // Если пешка превратилась — превращаем в ферзя (для простоты)
        if (game.pendingPromotion != null) {
            val (pr, pc) = game.pendingPromotion!!
            game.promotePawn(pr, pc, "${game.promotionColor}_queen")
        }
    }

    /**
     * Клонирует игру (создаёт независимую копию)
     * Нужно для анализа вариантов без изменения основной игры
     */
    private fun cloneGame(src: ChessGame): ChessGame {
        val dst = ChessGame()
        for (i in 0 until 8) for (j in 0 until 8) dst.board[i][j] = src.board[i][j]
        dst.currentPlayer    = src.currentPlayer
        dst.whiteKingPos     = src.whiteKingPos
        dst.blackKingPos     = src.blackKingPos
        dst.isCheck          = src.isCheck
        dst.isCheckmate      = src.isCheckmate
        dst.isStalemate      = src.isStalemate
        dst.pendingPromotion = src.pendingPromotion
        dst.promotionColor   = src.promotionColor
        dst.lastMove         = src.lastMove
        return dst
    }
}