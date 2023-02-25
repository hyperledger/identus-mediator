// import org.scalajs.dom.document

// import org.scalajs.dom._
// import scala.scalajs.js.JSON

// class ExampleJsDomTests extends munit.FunSuite {

//   test("Example jsdom test") {
//     val id = "my-fancy-element"
//     val content = "Hi there and greetings!"

//     // Create a new div element
//     val newDiv = document.createElement("div")

//     // Create an id attribute and assign it to the div
//     val a = document.createAttribute("id")
//     a.value = id
//     newDiv.setAttributeNode(a)

//     // Create some text content
//     val newContent = document.createTextNode(content)

//     // Add the text node to the newly created div
//     newDiv.appendChild(newContent)

//     // Add the newly created element and its content into the DOM
//     document.body.appendChild(newDiv)

//     // Find the element by id on the page, and compare the contents
//     assertEquals(document.getElementById(id).innerHTML, content)

//     // scala.scalajs.js.JavaScriptException: ReferenceError: fetch is not defined
//     def FIXME = fetch("https://fmgp.app", new RequestInit { method = HttpMethod.GET })

//     // FIXME

//     val xhr = new XMLHttpRequest()

//     xhr.open(
//       "GET",
//       "https://api.twitter.com/1.1/search/" +
//         "tweets.json?q=%23scalajs"
//     )
//     xhr.onload = { (e: Event) =>
//       if (xhr.status == 200) {
//         val r = JSON.parse(xhr.responseText)
//         // $("#tweets").html(parseTweets(r))
//       }
//     }
//     xhr.send()
//   }

// }
