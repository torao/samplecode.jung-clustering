package at.hazm.samplecode.jung.clustering

import edu.uci.ics.jung.algorithms.cluster.{EdgeBetweennessClusterer, VoltageClusterer}
import edu.uci.ics.jung.graph.DirectedSparseGraph
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.collection.JavaConverters._

/**
  * `EdgeBetweennessClusterer` を使用してグラフのクラスタリングを行う。
  * カレントディレクトリの `edge.txt` と `vertex.txt` を使用します。
  * {{{
  *   $ sbt run
  * }}}
  */
object Main {
  private[this] val logger = LoggerFactory.getLogger(getClass.getName.dropRight(1))

  case class Vertex(id:Int, name:String, count:Int)

  type Edge = String

  def main(args:Array[String]):Unit = {

    // 頂点の読み込み
    // [ID] [CATEGORY-NAME] [ARTICLE-COUNT]
    val vertices = Source.fromFile("vertex.txt").getLines().map { line =>
      val id :: name :: count :: Nil = line.split("\t").toList
      id.toInt -> Vertex(id.toInt, name, count.toInt)
    }.toMap

    val graph = new DirectedSparseGraph[Vertex, Edge]()
    vertices.values.foreach { vertex =>
      graph.addVertex(vertex)
    }

    // エッジの読み込みと設定
    // [ID] [ID] [UNIQUE-NAME]
    Source.fromFile("edge.txt").getLines().foreach { line =>
      val from :: to :: name :: Nil = line.split("\t").toList
      graph.addEdge(name, vertices(from.toInt), vertices(to.toInt))
    }

    /*
    val clusterer = new EdgeBetweennessClusterer[Vertex, Edge](50)
    val cluster = clusterer.apply(graph)
    */
    logger.info(f"clustering")
    val clusterer = new VoltageClusterer[Vertex, Edge](graph, 1000)
    val cluster = clusterer.cluster(1000)
    logger.info(f"finish")
    cluster.asScala.toSeq.map(c => (c, c.asScala.map(_.count).sum)).sortBy(- _._2).foreach{ case (cs, count) =>
      System.out.println(f"-- $count%,d --")
      cs.asScala.take(20).foreach{ c =>
        System.out.println(s"  ${c.id}: ${c.name}")
      }
    }
  }

}
