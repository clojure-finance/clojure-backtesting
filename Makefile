clean:
	clojure -T:build clean
install:
	clojure -T:build uber
start:
	clojure -M:gorilla
