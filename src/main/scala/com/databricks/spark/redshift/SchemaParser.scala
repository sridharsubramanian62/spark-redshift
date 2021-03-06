/*
 * Copyright 2014 Databricks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.databricks.spark.redshift

import scala.util.parsing.combinator._

import org.apache.spark.sql.types._

/**
 * A simple parser for Redshift table schemas.
 *
 * Note: the only method which uses this class has been deprecated, so this class should be
 * removed in `spark-redshift` 0.6. We will not accept patches to extend this parser.
 */
@deprecated("Do not use SchemaParser directly", "0.5.0")
private[redshift] object SchemaParser extends JavaTokenParsers {
  // redshift data types: http://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
  private val SMALLINT: Parser[DataType] = ("smallint" | "int2") ^^^ ShortType
  private val INTEGER: Parser[DataType] = ("integer" | "int" | "int4") ^^^ IntegerType
  private val BIGINT: Parser[DataType] = ("bigint" | "int8") ^^^ LongType
  private val DECIMAL: Parser[DataType] = // map all decimal to long for now
    ("decimal" | "numeric") ~ "(" ~ decimalNumber ~ "," ~ decimalNumber ~ ")" ^^^ LongType
  private val REAL: Parser[DataType] = ("real" | "float4") ^^^ FloatType
  private val DOUBLE: Parser[DataType] = ("double precision" | "float" | "float8") ^^^ DoubleType
  private val BOOLEAN: Parser[DataType] = "boolean" ^^^ BooleanType
  private val VARCHAR: Parser[DataType] =
    ("varchar" | "character varying" | "nvarchar"
      | "text" | "char" | "character"
      | "nchar" | "bpchar") ~ (("(" ~ decimalNumber ~ ")") | "") ^^^ StringType
  private val DATE: Parser[DataType] = "date" ^^^ DateType
  private val TIMESTAMP: Parser[DataType] =
    ("timestamp" | "timestamp without time zone") ^^^ TimestampType

  private val sqlType: Parser[DataType] =
    SMALLINT | INTEGER | BIGINT | DECIMAL | VARCHAR | DATE | BOOLEAN | REAL | DOUBLE | TIMESTAMP
  private val structField: Parser[StructField] = (ident ~ sqlType) ^^ {
    case colName ~ colType => StructField(colName, colType, nullable = true)
  }
  private val structType: Parser[StructType] = structField.* ^^ {
    case fields => StructType(fields)
  }

  def parseSchema(schema: String): StructType = {
    parse(structType, schema).get
  }
}
