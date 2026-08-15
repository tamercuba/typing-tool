{
  description = "Ambiente de desenvolvimento ClojureScript com Reagent e shadow-cljs";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = false;
        };
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            openjdk21
            clojure
            nodejs_24
            yarn
            rlwrap
            curl
          ];

          shellHook = ''
            if [ ! -d "node_modules/shadow-cljs" ]; then
              yarn install
            fi

            export PATH="$PWD/node_modules/.bin:$PATH"
          '';
        };
      }
    );
}
