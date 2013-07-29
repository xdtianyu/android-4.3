LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libceres

LOCAL_SDK_VERSION := 17
LOCAL_NDK_STL_VARIANT := stlport_static

LOCAL_C_INCLUDES := $(LOCAL_PATH)/internal \
                    $(LOCAL_PATH)/internal/ceres \
                    $(LOCAL_PATH)/include \
                    $(LOCAL_PATH)/internal/ceres/miniglog \
                    external/eigen

LOCAL_CPP_EXTENSION := .cc
LOCAL_CPPFLAGS := -DCERES_NO_PROTOCOL_BUFFERS \
                  -DCERES_NO_SUITESPARSE \
                  -DCERES_NO_GFLAGS \
                  -DCERES_NO_THREADS \
                  -DCERES_NO_CXSPARSE \
                  -DCERES_NO_TR1 \
                  -DCERES_WORK_AROUND_ANDROID_NDK_COMPILER_BUG \
                  -O3 -w

# On Android NDK 8b, GCC gives spurrious warnings about ABI incompatibility for
# which there is no solution. Hide the warning instead.
LOCAL_CPPFLAGS += -Wno-psabi

LOCAL_SRC_FILES := internal/ceres/array_utils.cc \
                   internal/ceres/block_evaluate_preparer.cc \
                   internal/ceres/block_jacobian_writer.cc \
                   internal/ceres/block_jacobi_preconditioner.cc \
                   internal/ceres/block_random_access_dense_matrix.cc \
                   internal/ceres/block_random_access_matrix.cc \
                   internal/ceres/block_random_access_sparse_matrix.cc \
                   internal/ceres/block_sparse_matrix.cc \
                   internal/ceres/block_structure.cc \
                   internal/ceres/canonical_views_clustering.cc \
                   internal/ceres/cgnr_solver.cc \
                   internal/ceres/compressed_row_jacobian_writer.cc \
                   internal/ceres/compressed_row_sparse_matrix.cc \
                   internal/ceres/conditioned_cost_function.cc \
                   internal/ceres/conjugate_gradients_solver.cc \
                   internal/ceres/coordinate_descent_minimizer.cc \
                   internal/ceres/corrector.cc \
                   internal/ceres/dense_normal_cholesky_solver.cc \
                   internal/ceres/dense_qr_solver.cc \
                   internal/ceres/dense_sparse_matrix.cc \
                   internal/ceres/detect_structure.cc \
                   internal/ceres/dogleg_strategy.cc \
                   internal/ceres/evaluator.cc \
                   internal/ceres/file.cc \
                   internal/ceres/gradient_checking_cost_function.cc \
                   internal/ceres/implicit_schur_complement.cc \
                   internal/ceres/iterative_schur_complement_solver.cc \
                   internal/ceres/levenberg_marquardt_strategy.cc \
                   internal/ceres/linear_least_squares_problems.cc \
                   internal/ceres/linear_operator.cc \
                   internal/ceres/linear_solver.cc \
                   internal/ceres/local_parameterization.cc \
                   internal/ceres/loss_function.cc \
                   internal/ceres/miniglog/glog/logging.cc \
                   internal/ceres/normal_prior.cc \
                   internal/ceres/parameter_block_ordering.cc \
                   internal/ceres/partitioned_matrix_view.cc \
                   internal/ceres/polynomial_solver.cc \
                   internal/ceres/problem.cc \
                   internal/ceres/problem_impl.cc \
                   internal/ceres/program.cc \
                   internal/ceres/residual_block.cc \
                   internal/ceres/residual_block_utils.cc \
                   internal/ceres/runtime_numeric_diff_cost_function.cc \
                   internal/ceres/schur_complement_solver.cc \
                   internal/ceres/schur_eliminator.cc \
                   internal/ceres/scratch_evaluate_preparer.cc \
                   internal/ceres/solver.cc \
                   internal/ceres/solver_impl.cc \
                   internal/ceres/sparse_matrix.cc \
                   internal/ceres/sparse_normal_cholesky_solver.cc \
                   internal/ceres/split.cc \
                   internal/ceres/stringprintf.cc \
                   internal/ceres/suitesparse.cc \
                   internal/ceres/triplet_sparse_matrix.cc \
                   internal/ceres/trust_region_minimizer.cc \
                   internal/ceres/trust_region_strategy.cc \
                   internal/ceres/types.cc \
                   internal/ceres/visibility_based_preconditioner.cc \
                   internal/ceres/visibility.cc \
                   internal/ceres/wall_time.cc \
                   internal/ceres/generated/schur_eliminator_d_d_d.cc \
                   internal/ceres/generated/schur_eliminator_2_2_2.cc \
                   internal/ceres/generated/schur_eliminator_2_2_3.cc \
                   internal/ceres/generated/schur_eliminator_2_2_4.cc \
                   internal/ceres/generated/schur_eliminator_2_2_d.cc \
                   internal/ceres/generated/schur_eliminator_2_3_3.cc \
                   internal/ceres/generated/schur_eliminator_2_3_4.cc \
                   internal/ceres/generated/schur_eliminator_2_3_9.cc \
                   internal/ceres/generated/schur_eliminator_2_3_d.cc \
                   internal/ceres/generated/schur_eliminator_2_4_3.cc \
                   internal/ceres/generated/schur_eliminator_2_4_4.cc \
                   internal/ceres/generated/schur_eliminator_2_4_d.cc \
                   internal/ceres/generated/schur_eliminator_4_4_2.cc \
                   internal/ceres/generated/schur_eliminator_4_4_3.cc \
                   internal/ceres/generated/schur_eliminator_4_4_4.cc \
                   internal/ceres/generated/schur_eliminator_4_4_d.cc

include $(BUILD_STATIC_LIBRARY)
