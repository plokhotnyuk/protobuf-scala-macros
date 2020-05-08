module SchemaPull
  ( encodeTestSchema
  ) where

import Data.Array (concatMap)
import Data.Tuple (Tuple(Tuple))
import Prelude (($))
import Proto.Encode as Encode
import Proto.Uint8Array (Uint8Array, length, concatAll)
import SchemaCommon



encodeTestSchema :: TestSchema -> Uint8Array
encodeTestSchema (ClassWithMap x) = concatAll [ Encode.uint32 10, encodeClassWithMap x ]

encodeClassWithMap :: ClassWithMap -> Uint8Array
encodeClassWithMap msg = do
  let xs = concatAll
        [ concatAll $ concatMap (\x -> [ Encode.uint32 10, encodeStringString x ]) msg.m
        ]
  concatAll [ Encode.uint32 $ length xs, xs ]

encodeStringString :: Tuple String String -> Uint8Array
encodeStringString (Tuple _1 _2) = do
  let msg = { _1, _2 }
  let xs = concatAll
        [ Encode.uint32 10
        , Encode.string msg._1
        , Encode.uint32 18
        , Encode.string msg._2
        ]
  concatAll [ Encode.uint32 $ length xs, xs ]