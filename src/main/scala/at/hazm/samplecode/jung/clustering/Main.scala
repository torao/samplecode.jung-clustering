package at.hazm.samplecode.jung.clustering

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets

import edu.uci.ics.jung.algorithms.cluster.{EdgeBetweennessClusterer, VoltageClusterer}
import edu.uci.ics.jung.graph.DirectedSparseGraph
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.collection.JavaConverters._
import scala.collection.mutable

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
    val numCluster = 100
    val dst = new File(s"cluster_$numCluster.txt")

    if(!dst.isFile) {

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

      logger.info(f"start clustering")
      val t0 = System.currentTimeMillis()
      val clusterer = new EdgeBetweennessClusterer[Vertex, Edge](5000)
      val cluster = clusterer.apply(graph)
//      val clusterer = new VoltageClusterer[Vertex, Edge](graph, numCluster)
//      val cluster = clusterer.cluster(numCluster)
      logger.info(f"finish clustering ${System.currentTimeMillis() - t0}%,dms")
      val out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dst), StandardCharsets.UTF_8))
      cluster.asScala.toSeq.map(cs => (cs, cs.asScala.map(_.count).sum)).sortBy(-_._2).zipWithIndex.foreach { case ((cats, articles), cid) =>
        cats.asScala.take(20).foreach { c =>
          out.println(s"$cid\t${c.id}\t${c.name}\t${c.count}")
        }
      }
      out.close()
    }
    val cluster = Source.fromFile(dst).getLines().foldLeft(mutable.HashMap[Int, mutable.HashSet[Vertex]]()) { case (map, line) =>
      val cid :: id :: name :: count :: Nil = line.split("\t").toList
      val set = map.getOrElseUpdate(cid.toInt, mutable.HashSet[Vertex]())
      set.add(Vertex(id.toInt, name, count.toInt))
      map
    }.values.map(_.toSet).toSet
    cluster.toSeq.map(cs => (cs, cs.map(_.count).sum)).sortBy(-_._2).foreach { case (cats, articles) =>
      System.out.println(f"-- $articles%,d --")
      cats.take(20).foreach { c =>
        System.out.println(f"  ${c.id}: ${c.name} (${c.count}%,d)")
      }
    }
  }

}
