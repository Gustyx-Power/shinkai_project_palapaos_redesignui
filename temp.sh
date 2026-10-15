#!/bin/bash
set -e


# Setup environment
source build/envsetup.sh

# Lunch target (tanpa trunking / menggunakan target rilis cp2a)
breakfast marble

# Disable RBE to prevent reproxy timeouts and force full local 96-core compilation
export USE_RBE=false

# Build ROM (Full Clock / All Cores)
m shinkai -j$(nproc)
