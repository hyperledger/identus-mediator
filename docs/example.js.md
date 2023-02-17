# Example JS

To install my project

```scala #mdoc:js
import org.scalajs.dom.window._
import jsdocs._

val progress = new Progress()
node.innerHTML = progress.tick(5)
setInterval({ () =>
  // `node` variable is a DOM element in scope.
  node.innerHTML = progress.tick(5)
}, 100)
```