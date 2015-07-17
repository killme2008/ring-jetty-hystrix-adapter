# ring-jetty-hystrix-adapter

Setup a Hystrix (https://github.com/Netflix/Hystrix) event stream with jetty for clojure.

## Usage

Leiningen

```clj
[ring-jetty-hystrix-adapter "0.1.0"]
```

Run a jetty server:

```clj
(require '[ring-jetty-hystrix-adapter.core :as jetty])

(jetty/run-jetty-with-hystrix {:port 3000
                               :max-threads 10
                               :hystrix-servlet-path "/hystrix.stream"
                               :join? false})
```

Just like [ring-jetty-adpater]() but has a new option `hystrix-servlet-path` to
export hystrix event stream. Also see [hystrix-event-stream-clj](https://github.com/josephwilk/hystrix-event-stream-clj).

## License

Copyright © 2015 killme2008

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
