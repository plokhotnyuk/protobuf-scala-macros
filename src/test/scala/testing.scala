package zd.proto.test

import org.scalatest.FreeSpec
import zd.proto.api.{MessageCodec, encode, decode, N}
import zd.proto.macrosapi.{messageCodecIdx, messageCodecNums, messageCodecAuto, sealedTraitCodecAuto, sealedTraitCodecNums}

object models {
  final case class Basic(
    @N(21) int: Int
  , @N(22) long: Long
  , @N(23) bool: Boolean
  , @N(24) double: Double
  , @N(25) float: Float
  , @N(26) str: String
  , @N(50) bytes: Array[Byte]
  )

  final case class OptionBasic( 
    @N(21) int: Option[Int]
  , @N(22) long: Option[Long]
  , @N(23) bool: Option[Boolean]
  , @N(24) double: Option[Double]
  , @N(25) float: Option[Float]
  , @N(26) str: Option[String]
  , @N(50) bytes: Option[Array[Byte]]
  )

  final case class Collections(
    @N(21) int: List[Int]
  , @N(22) long: List[Long]
  , @N(23) bool: List[Boolean]
  , @N(24) double: List[Double]
  , @N(25) float: List[Float]
  , @N(26) str: List[String]
  , @N(27) bytes: List[Array[Byte]]
  , @N(28) message: List[Vehicle]
  , @N(29) basicBasic: Map[Int, Int]
  , @N(30) basisMessage: Map[String, Vehicle]
  , @N(31) messageMessage: Map[Vehicle, Vehicle]
  , @N(32) messageBasic: Map[Vehicle, String]
  , @N(33) setBasic: Set[Double]
  , @N(50) setMessage: Set[Vehicle]
  )

  sealed trait Vehicle
  @N(21) final case class Car(@N(21) id: String) extends Vehicle
  @N(22) final case class Bus(@N(21) id: String) extends Vehicle
  @N(50) final case object Unknown extends Vehicle

  final case class Parking(@N(21) place1: Option[Vehicle], @N(22) place2: Option[Vehicle], @N(23) other: List[Vehicle], @N(50) reserved: Vehicle)

  final case class ShoppingMall(@N(21) parking1: Option[Parking], @N(50) parking2: Option[Parking])
}

class testing extends FreeSpec {
  import models._

  "basic" - {
    def test(implicit codec: MessageCodec[Basic]): Unit = {
      (for {
        int <- List(Int.MinValue, -2, -1, 0, 1, 2, Int.MaxValue)
        long <- List(Long.MinValue, -2L, -1L, 0L, 1L, 2L, Long.MaxValue)
        bool <- List(false, true)
        double <- List(Double.MinValue, -2.0D, -1.0D, 0.0D, 1.0D, 2.0D, Double.MaxValue)
        float <- List(Float.MinValue, -2.0F, -1.0F, 0.0F, 1.0F, 2.0F, Float.MaxValue)
        str <- List("", "str")
        bytes <- List(Array.empty[Byte], Array(0.toByte), Array(1.toByte), Array(2.toByte), Array(255.toByte))
      } yield Basic(int = int, long = long, bool = bool, double = double, float = float, str = str, bytes = bytes)).foreach{ data =>
        val decoded = decode(encode(data))
        assert(decoded.int === data.int)
        assert(decoded.long === data.long)
        assert(decoded.bool === data.bool)
        assert(decoded.double === data.double)
        assert(decoded.float === data.float)
        assert(decoded.str === data.str)
        assert(decoded.bytes === data.bytes)
      }
    }
    "encode <-> decode" - {
      "auto codec" in { implicit val codec = messageCodecAuto[Basic]; test }
      "nums codec" in { implicit val codec = messageCodecNums[Basic]('int->6, 'long->7, 'bool->8, 'double->9, 'float->10, 'str->20, 'bytes->21); test }
      "idx codec" in { implicit val codec = messageCodecIdx[Basic]; test }
    }
  }

  "option basic" - {
    object autocodec { implicit val codec = messageCodecAuto[OptionBasic] }
    object numscodec { implicit val codec = messageCodecNums[OptionBasic]('int->6, 'long->7, 'bool->8, 'double->9, 'float->10, 'str->20, 'bytes->21) }
    object idxcodec { implicit val codec = messageCodecIdx[OptionBasic] }

    "empty bytearray <-> all fields none" - {
      val data = OptionBasic(int = None, long = None, bool = None, double = None, float = None, str = None, bytes = None)
      def test(implicit codec: MessageCodec[OptionBasic]): Unit = {
        assert(encode(data) === Array.empty[Byte])
        val decoded = decode[OptionBasic](Array.empty[Byte])
        val _ = assert(decoded === data)
      }
      "auto codec" in { import autocodec._; test }
      "nums codec" in { import numscodec._; test }
      "idx codec" in { import idxcodec._; test }
    }

    "bytearray <-> all fields" - {
      val data = OptionBasic(int = Some(1), long = Some(2L), bool = Some(true), double = Some(3.0D), float = Some(4.0F), str = Some("5"), bytes = Some(Array(6, 7, 8, 9, 0)))
      "auto codec" in {
        import autocodec._

        val bytes = Array[Byte](168.toByte,1,1, 176.toByte,1,2, 184.toByte,1,1, 193.toByte,1,0,0,0,0,0,0,8,64, 205.toByte,1,0,0,128.toByte,64, 210.toByte,1,1,53, 146.toByte,3,5,6,7,8,9,0)
        assert(encode(data) === bytes)
        val decoded = decode[OptionBasic](bytes)
        assert(decoded.int.get === data.int.get)
        assert(decoded.long.get === data.long.get)
        assert(decoded.bool.get === data.bool.get)
        assert(decoded.double.get === data.double.get)
        assert(decoded.float.get === data.float.get)
        assert(decoded.str.get === data.str.get)
        assert(decoded.bytes.get === data.bytes.get)
      }
      "nums codec" in { 
        import numscodec._
        val bytes = Array[Byte](48,1, 56,2, 64,1, 73,0,0,0,0,0,0,8,64, 85,0,0,128.toByte,64, 162.toByte,1,1,53, 170.toByte,1,5,6,7,8,9,0)
        assert(encode(data) === bytes)
        val decoded = decode[OptionBasic](bytes)
        assert(decoded.int.get === data.int.get)
        assert(decoded.long.get === data.long.get)
        assert(decoded.bool.get === data.bool.get)
        assert(decoded.double.get === data.double.get)
        assert(decoded.float.get === data.float.get)
        assert(decoded.str.get === data.str.get)
        assert(decoded.bytes.get === data.bytes.get)
      }
      "idx codec" in {
        import idxcodec._
        val bytes = Array[Byte](8,1, 16,2, 24,1, 33,0,0,0,0,0,0,8,64, 45,0,0,128.toByte,64, 50,1,53, 58,5,6,7,8,9,0)
        assert(encode(data) === bytes)
        val decoded = decode[OptionBasic](bytes)
        assert(decoded.int.get === data.int.get)
        assert(decoded.long.get === data.long.get)
        assert(decoded.bool.get === data.bool.get)
        assert(decoded.double.get === data.double.get)
        assert(decoded.float.get === data.float.get)
        assert(decoded.str.get === data.str.get)
        assert(decoded.bytes.get === data.bytes.get)
      }
    }
  }

  object messages {
    object autocodec {
      implicit val vehicleCodec: MessageCodec[Vehicle] = {
        implicit val carCodec = messageCodecAuto[Car]
        implicit val busCodec = messageCodecAuto[Bus]
        implicit val unknownCodec = messageCodecAuto[Unknown.type]
        sealedTraitCodecAuto[Vehicle]
      }
      implicit val parkingCodec = messageCodecAuto[Parking]
      implicit val shoppingMallCodec = messageCodecAuto[ShoppingMall]
    }
    object numscodec {
      implicit val vehicleCodec: MessageCodec[Vehicle] = {
        implicit val carCodec = messageCodecNums[Car]('id->2)
        implicit val busCodec = messageCodecNums[Bus]('id->2)
        implicit val unknownCodec = messageCodecNums[Unknown.type]()
        sealedTraitCodecNums[Vehicle]('Car->10, 'Bus->22, 'Unknown->51)
      }
      implicit val parkingCodec = messageCodecNums[Parking]('place1->1, 'place2->2, 'other->3, 'reserved->4)
      implicit val shoppingMallCodec = messageCodecNums[ShoppingMall]('parking1->1, 'parking2->2)
    }
    object idxcodec {
      implicit val vehicleCodec: MessageCodec[Vehicle] = {
        implicit val carCodec = messageCodecIdx[Car]
        implicit val busCodec = messageCodecIdx[Bus]
        implicit val unknownCodec = messageCodecIdx[Unknown.type]
        sealedTraitCodecNums[Vehicle]('Car->10, 'Bus->22, 'Unknown->51)
      }
      implicit val parkingCodec = messageCodecIdx[Parking]
      implicit val shoppingMallCodec = messageCodecIdx[ShoppingMall]
    }
  }

  object collections {
    object autocodec {
      import messages.autocodec._
      implicit val tuple2IntInt = messageCodecIdx[Tuple2[Int, Int]]
      implicit def tuple2StringA[A:MessageCodec] = messageCodecIdx[Tuple2[String, A]]
      implicit def tuple2AString[A:MessageCodec] = messageCodecIdx[Tuple2[A, String]]
      implicit def tuple2AB[A:MessageCodec, B:MessageCodec] = messageCodecIdx[Tuple2[A, B]]
      implicit val collectionCodec: MessageCodec[Collections] = messageCodecAuto[Collections]
    }
    object numscodec {
      import messages.numscodec._
      implicit val tuple2IntInt = messageCodecNums[Tuple2[Int, Int]]('_1->1, '_2->2)
      implicit def tuple2StringA[A:MessageCodec] = messageCodecNums[Tuple2[String, A]]('_1->1, '_2->2)
      implicit def tuple2AString[A:MessageCodec] = messageCodecNums[Tuple2[A, String]]('_1->1, '_2->2)
      implicit def tuple2AB[A:MessageCodec, B:MessageCodec] = messageCodecNums[Tuple2[A, B]]('_1->1, '_2->2)
      implicit val collectionCodec: MessageCodec[Collections] = messageCodecNums[Collections](
        'int->7
      , 'long->8
      , 'bool->9
      , 'double->10
      , 'float->11
      , 'str->12
      , 'bytes->13
      , 'message->14
      , 'basicBasic->15
      , 'basisMessage->16
      , 'messageMessage->17
      , 'messageBasic->18
      , 'setBasic->19
      , 'setMessage->20
      )
    }
    object idxcodec {
      import messages.idxcodec._
      implicit val tuple2IntInt = messageCodecIdx[Tuple2[Int, Int]]
      implicit def tuple2StringA[A:MessageCodec] = messageCodecIdx[Tuple2[String, A]]
      implicit def tuple2AString[A:MessageCodec] = messageCodecIdx[Tuple2[A, String]]
      implicit def tuple2AB[A:MessageCodec, B:MessageCodec] = messageCodecIdx[Tuple2[A, B]]
      implicit val collectionCodec: MessageCodec[Collections] = messageCodecIdx[Collections]
    }
  }

  "collections" - {
    def test(implicit codec: MessageCodec[Collections]): Unit = {
      val int = List(Int.MinValue, -2, -1, 0, 1, 2, Int.MaxValue)
      val long = List(Long.MinValue, -2L, -1L, 0L, 1L, 2L, Long.MaxValue)
      val bool = List(false, true)
      val double = List(Double.MinValue, -2.0D, -1.0D, 0.0D, 1.0D, 2.0D, Double.MaxValue)
      val float = List(Float.MinValue, -2.0F, -1.0F, 0.0F, 1.0F, 2.0F, Float.MaxValue)
      val str = List("", "str")
      val bytes = List(Array.empty[Byte], Array(0.toByte), Array(1.toByte), Array(2.toByte), Array(255.toByte))
      val message = List(Car(id = "123"), Bus(id = "456"), Unknown, Car(id = "789"))
      val basicBasic: Map[Int, Int] = Map(1->2, 2->3, 4->5)
      val basisMessage: Map[String, Vehicle] = Map("1"->Car(id="1"), "2"->Bus(id="2"), "3"->Unknown)
      val messageMessage: Map[Vehicle, Vehicle] = Map(Bus(id="1")->Unknown, Bus(id="2")->Bus(id="3"))
      val messageBasic: Map[Vehicle, String] = Map(Bus(id="1")->"123", Car("2")->"456")
      val setBasic: Set[Double] = Set(Double.MinValue, -2.0D, -1.1D, 0.0D, 1.1D, 2.0D, Double.MaxValue)
      val setMessage: Set[Vehicle] = Set(Car(id="1"), Bus(id="2"), Unknown)
      val data = Collections(
        int = int
      , long = long
      , bool = bool
      , double = double
      , float = float
      , str = str
      , bytes = bytes
      , message = message
      , basicBasic = basicBasic
      , basisMessage = basisMessage
      , messageMessage = messageMessage
      , messageBasic = messageBasic
      , setBasic = setBasic
      , setMessage = setMessage
      )
      val decoded = decode(encode(data))
      assert(decoded.int === data.int)
      assert(decoded.long === data.long)
      assert(decoded.bool === data.bool)
      assert(decoded.double === data.double)
      assert(decoded.float === data.float)
      assert(decoded.str === data.str)
      val _ = assert(decoded.bytes.zip(data.bytes).forall{ case (decodedBytes, dataBytes) => decodedBytes === dataBytes })
    }
    "encode <-> decode" - {
      "auto codec" in { import collections.autocodec._; test }
      "nums codec" in { import collections.numscodec._; test }
      "idx codec" in { import collections.idxcodec._; test }
    }
  }

  "message" - {
    "encode <-> decode" - {
      def test(implicit codec: MessageCodec[ShoppingMall]): Unit = {
        val parking = Parking(place1=None, place2=Some(Car(id="123")), other=List(Car(id="456"), Bus(id="789"), Unknown), reserved=Car(id="0"))
        val data = ShoppingMall(parking1=None, parking2=Some(parking))
        val decoded = decode[ShoppingMall](encode(data))
        val _ = assert(decoded === data)
      }
      "auto codec" in { import messages.autocodec._; test }
      "nums codec" in { import messages.numscodec._; test }
      "idx codec" in { import messages.idxcodec._; test }
    }
  }
}
