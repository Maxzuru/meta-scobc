# Developed with reference to nix-community/nix-environments:
# https://github.com/nix-community/nix-environments
{
  description = "FHS build environment for meta-scobc (Xilinx Yocto 2025.2)";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";

  outputs =
    { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      inherit (pkgs) lib;
      cc = pkgs.gcc13;

      # BitBake sanitizes the environment. Keep the Nix wrapper settings in
      # host tool launchers instead of exporting them to every recipe through
      # BBPOSTCONF (including the MicroBlaze bare-metal multiconfigs).
      hostTool =
        package: name:
        pkgs.writeShellScriptBin name ''
          export NIX_CC_WRAPPER_TARGET_HOST_${cc.suffixSalt}=1
          export NIX_BINTOOLS_WRAPPER_TARGET_HOST_${cc.suffixSalt}=1
          export NIX_CFLAGS_COMPILE="-idirafter /usr/include"
          export NIX_CFLAGS_LINK="-L/usr/lib"
          export NIX_LDFLAGS="-L/usr/lib"
          export NIX_DYNAMIC_LINKER_${cc.suffixSalt}=/lib/ld-linux-x86-64.so.2
          export NIX_DONT_SET_RPATH=1
          unset NIX_ENFORCE_PURITY
          exec ${package}/bin/${name} "$@"
        '';
      hostTools = pkgs.symlinkJoin {
        name = "yocto-host-tools";
        paths =
          (map (hostTool cc) [
            "gcc"
            "g++"
            "cc"
            "c++"
            "cpp"
          ])
          ++ (map (hostTool cc.bintools) [
            "ld"
            "ld.bfd"
          ]);
      };
      # Nixpkgs' rpcgen reads CPP as an executable, but BitBake exports a
      # complete cross-preprocessor command there. Like the Ubuntu host tool,
      # use the host cpp for RPC protocol generation; allow an explicit override.
      rpcgen = pkgs.writeShellScriptBin "rpcgen" ''
        export RPCGEN_CPP="''${RPCGEN_CPP:-/usr/bin/cpp}"
        exec ${pkgs.rpcsvc-proto}/bin/rpcgen "$@"
      '';

      # Xilinx binaries may still need ABI 5, alongside Yocto's ABI 6 tools.
      ncurses5 = pkgs.ncurses5.overrideAttrs (old: {
        configureFlags = old.configureFlags ++ [ "--with-termlib" ];
        postFixup = "";
      });
      lz4 = pkgs.lz4.overrideAttrs (old: {
        postInstall = (old.postInstall or "") + ''
          ln -sf lz4 "$out/bin/lz4c"
        '';
      });

      fhsArgs = {
        name = "scobc-yocto";
        targetPkgs =
          p: with p; [
            (lib.hiPrio hostTools)
            (lib.hiPrio rpcgen)
            cc
            cc.bintools
            acl
            attr
            bc
            bison
            chrpath
            cpio
            curl
            diffstat
            dtc
            pahole
            elfutils
            expect
            file
            flex
            git
            git-lfs
            git-repo
            gnumake
            hostname
            iproute2
            iputils
            jq
            kas
            kmod
            # cmake-native restored from the shared sstate needs this.
            libidn2
            libxcrypt
            libxcrypt-legacy
            lz4
            ncurses
            ncurses5
            openssh
            openssl
            patch
            perl
            pkg-config
            procps
            (python3.withPackages (ps: [ ps.pyyaml ]))
            rpcsvc-proto
            rsync
            socat
            texinfo
            ubootTools
            unzip
            util-linux
            uv
            wget
            which
            xz
            zlib
            zstd
            xorg.libX11
            xorg.libXext
            xorg.libXrender
            xorg.libXi
            xorg.libXtst
            xorg.libxcb
          ];
        multiPkgs = _: [ ];
        extraOutputsToInstall = [ "dev" ];
        profile = ''
          # Prefer each Yocto tool's own RUNPATH over host libraries.
          unset LD_LIBRARY_PATH
          # buildFHSEnv sets these globally; the host launchers above supply
          # them locally even after BitBake has sanitized the environment.
          unset NIX_CFLAGS_COMPILE NIX_CFLAGS_LINK NIX_LDFLAGS
          unset NIX_CC_WRAPPER_TARGET_HOST_${pkgs.stdenv.cc.suffixSalt}
          unset NIX_BINTOOLS_WRAPPER_TARGET_HOST_${pkgs.stdenv.cc.suffixSalt}
          export LANG=en_US.UTF-8
          export LC_ALL=en_US.UTF-8
          export LOCALE_ARCHIVE=/usr/lib/locale/locale-archive
          # Prevent kas from passing Nix's temporary directory to BitBake as
          # its build directory.
          unset TMPDIR
        '';
        runScript = "bash";
      };
      fhs = pkgs.buildFHSEnv fhsArgs;
      debugFhs = pkgs.buildFHSEnv (
        fhsArgs
        // {
          name = "scobc-yocto-debug";
          runScript = pkgs.writeShellScript "yocto-debug-shell" ''
            # Keep Bash's arguments (such as -c) out of the sourced setup script.
            script_dir=$(cd -- ./scripts && pwd) || exit 1
            source "$script_dir/sc-init-build-env" "''${KAS_BUILD_DIR:-$script_dir/../build}" || exit 1
            exec bash --norc "$@"
          '';
        }
      );
    in
    {
      devShells.${system} = {
        default = fhs.env;
        debug = debugFhs.env;
      };
      packages.${system} = {
        default = fhs;
        debug = debugFhs;
      };
      apps.${system} = {
        default = {
          type = "app";
          program = "${fhs}/bin/scobc-yocto";
          meta.description = "Enter the meta-scobc Yocto FHS build environment";
        };
        debug = {
          type = "app";
          program = "${debugFhs}/bin/scobc-yocto-debug";
          meta.description = "Enter an initialized Yocto environment for direct BitBake commands";
        };
      };
      formatter.${system} = pkgs.nixfmt-rfc-style;
    };
}
