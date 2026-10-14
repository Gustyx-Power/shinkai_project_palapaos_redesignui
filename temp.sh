#!/bin/bash
set -e


# Setup environment
source build/envsetup.sh

# Lunch target (tanpa trunking / menggunakan target rilis cp2a)
breakfast marble

# Build ROM (Full Clock / All Cores)
m shinkai -j$(nproc)
