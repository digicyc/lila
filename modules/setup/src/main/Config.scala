package lila.setup

import chess.format.Forsyth
import chess.{ Game => ChessGame, Board, Situation, Variant, Clock, Speed }

import lila.game.{ GameRepo, Game, Pov }
import lila.lobby.Color
import lila.tournament.{ System => TournamentSystem }

private[setup] trait Config {

  // Whether or not to use a clock
  val timeMode: TimeMode

  // Clock time in minutes
  val time: Int

  // Clock increment in seconds
  val increment: Int

  // Correspondence days per turn
  val days: Int

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  def hasClock = timeMode == TimeMode.RealTime

  lazy val creatorColor = color.resolve

  def makeGame = ChessGame(board = Board init variant, clock = makeClock)

  def validClock = hasClock.fold(clockHasTime, true)

  def clockHasTime = time + increment > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock =
    Clock(time * 60, clockHasTime.fold(increment, 1))

  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait GameGenerator { self: Config =>

  def game: Game

  def pov = Pov(game, creatorColor)
}

trait Positional { self: Config =>

  import chess.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[String]

  def strictFen: Boolean

  lazy val validFen = variant != Variant.FromPosition || {
    fen ?? { f => ~(Forsyth <<< f).map(_.situation playable strictFen) }
  }

  def fenGame(builder: ChessGame => Game): Game = {
    val state = fen ifTrue (variant == Variant.FromPosition) flatMap Forsyth.<<<
    val chessGame = state.fold(makeGame) {
      case sit@SituationPlus(Situation(board, color), _) =>
        ChessGame(
          board = board,
          player = color,
          turns = sit.turns,
          startedAtTurn = sit.turns,
          clock = makeClock)
    }
    val game = builder(chessGame)
    state.fold(game) {
      case sit@SituationPlus(Situation(board, _), _) => game.copy(
        variant = Variant.FromPosition,
        castleLastMoveTime = game.castleLastMoveTime.copy(
          lastMove = board.history.lastMove,
          castles = board.history.castles
        ),
        turns = sit.turns)
    }
  }
}

object Config extends BaseConfig

trait BaseConfig {
  val systems = List(TournamentSystem.Arena.id, TournamentSystem.Swiss.id)
  val systemDefault = TournamentSystem.default

  val variants = List(Variant.Standard.id, Variant.Chess960.id)
  val variantDefault = Variant.Standard

  val variantsWithFen = variants :+ Variant.FromPosition.id
  val variantsWithFenAndKingOfTheHill = variants :+ Variant.KingOfTheHill.id :+ Variant.FromPosition.id
  val variantsWithVariants = variants :+ Variant.KingOfTheHill.id :+ Variant.ThreeCheck.id :+ Variant.Antichess.id
  val variantsWithFenAndVariants =
    variants :+ Variant.KingOfTheHill.id :+ Variant.ThreeCheck.id :+ Variant.Antichess.id :+ Variant.FromPosition.id

  val speeds = Speed.all map (_.id)

  private val timeMin = 0
  private val timeMax = 180
  def validateTime(t: Int) = t >= timeMin && t <= timeMax

  private val incrementMin = 0
  private val incrementMax = 180
  def validateIncrement(i: Int) = i >= incrementMin && i <= incrementMax
}
