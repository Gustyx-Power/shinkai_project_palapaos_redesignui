#!/bin/bash
set -e

export GOMEMLIMIT=10GiB
export GOMAXPROCS=8

# Setup environment
source build/envsetup.sh

# Lunch target (tanpa trunking / menggunakan target rilis cp2a)
breakfast marble

# Build ROM (cap parallelism to 32 to keep peak RAM under 48GB cgroup limit)
m shinkai -j32
