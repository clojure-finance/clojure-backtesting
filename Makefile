clean:
	lein clean
init_clojupyter:
	cd clojupyter; make
remove_kernel:
	-lein clojupyter remove-install backtesting_clojure
add_kernel:
	lein uberjar;
	make remove_kernel;
	lein clojupyter install --ident backtesting_clojure --jarfile target/uberjar/clojure-backtesting-0.1.0-SNAPSHOT-standalone.jar;

