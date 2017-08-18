package at.hazm.samplecode.jung.clustering

import java.io._
import java.nio.charset.StandardCharsets

import scala.collection.mutable
import scala.io.Source

/**
  * 1行目にヘッダ行を持つタブ区切りの [PAGE-ID] [TITLE] [REDIRECT] [CATEGORY]* 形式ファイルを読み込んでグラフ処理用の
  * edge.txt と vertex.txt を作成します。
  */
object Convert {

  case class Page(id:Int, prefix:Option[String], title:String, redirect:Option[String], categories:Seq[String])

  class Category(val page:Page) {
    var count:Int = 0
  }

  def main(args:Array[String]):Unit = {
    val src = new File(args.head)
    val edgeFile = new File("edge.txt")
    val vertexFile = new File("vertex.txt")

    // すべてのページ定義を読み込み
    val all = Source.fromFile(src, "UTF-8").getLines.drop(1).map { line =>
      val id :: rawTitle :: red :: categories = line.split("\t").toList.padTo(4, "")
      val (prefix, title) = rawTitle.toUpperCase.split(":", 2) match {
        case Array(p, t) => (Some(p), t)
        case Array(t) => (None, t)
      }
      Page(id.toInt, prefix, title, if(red.isEmpty) None else Some(red.toUpperCase),
        categories.filter(_.nonEmpty).map(_.toUpperCase))
    }.toList

    // グラフの頂点として扱うカテゴリを抽出 (重複したタイトルを持つページが存在する)
    val vertex = all.collect {
      case p if p.prefix.contains("CATEGORY") =>
        new Category(p)
    }
    val vertexIndex = vertex.map(v => (v.page.title, v)).toMap

    // カテゴリリダイレクトの解決
    def resolve(title:String):Option[Category] = vertexIndex.get(title).flatMap { cat =>
      cat.page.redirect match {
        case Some(redirect) if redirect.startsWith("CATEGORY:") =>
          vertexIndex.get(redirect.substring(9)).flatMap(c => resolve(c.page.title))
        case None => Some(cat)
      }
    }

    // カテゴリに属する記事数を設定
    all.filter(a => a.prefix.isEmpty && a.redirect.isEmpty).foreach { article =>
      article.categories.foreach { category =>
        resolve(category) match {
          case Some(c) => c.count += 1
          case None =>
            System.out.println(s"Category or Redirect Not Found: $category from article ${article.title}")
        }
      }
    }

    // タイトル重複も含むすべてのカテゴリを出力
    val vertexOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(vertexFile), StandardCharsets.UTF_8))
    vertex.filter(_.page.redirect.isEmpty).sortBy(_.page.id).foreach { c =>
      vertexOut.println(s"${c.page.id}\t${c.page.title}\t${c.count}")
    }
    vertexOut.close()

    // すべてのカテゴリ間の関連からリダイレクトを除外して出力
    val uniqueNames = mutable.HashMap[String,Int]()
    val edgeOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(edgeFile), StandardCharsets.UTF_8))
    vertex.filter(_.page.redirect.isEmpty).flatMap { cat =>
      cat.page.categories.flatMap(resolve).map { c => (cat.page.id, c.page.id, s"${cat.page.title}:${c.page.title}") }
    }.sortBy(_._1).foreach { case (child, parent, name) =>
      val i = uniqueNames.getOrElseUpdate(name, 0)
      uniqueNames.update(name, i+1)
      edgeOut.println(s"$child\t$parent\t$name${if(i==0) "" else s"__$i"}")
    }
    edgeOut.close()
  }
}
