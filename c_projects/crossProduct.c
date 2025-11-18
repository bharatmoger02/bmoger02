#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);

    int world_rank, world_size;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);

    int N = 0; // Number of vectors (We will input this)

    // --- CHANGE 1: Rank 0 asks for the number of vectors ---
    if (world_rank == 0) {
        printf("Enter the number of vectors (Must be divisible by %d): ", world_size);
        fflush(stdout); // Force the print to appear before scanf
        scanf("%d", &N);
    }

    // --- CHANGE 2: Broadcast N to all processes ---
    // Rank 0 reads N, but Rank 1, 2, 3 don't know what N is yet.
    // MPI_Bcast sends the value of N from Rank 0 to everyone.
    MPI_Bcast(&N, 1, MPI_INT, 0, MPI_COMM_WORLD);

    // Check if N is valid for equal scattering
    if (N % world_size != 0) {
        if (world_rank == 0) {
            printf("Error: Number of vectors (%d) must be divisible by number of processors (%d).\n", N, world_size);
        }
        MPI_Finalize();
        return 0;
    }

    int total_elements = N * 3;
    int elements_per_proc = total_elements / world_size;

    float *A = NULL;
    float *B = NULL;
    float *C = NULL;

    float *sub_A = (float *)malloc(sizeof(float) * elements_per_proc);
    float *sub_B = (float *)malloc(sizeof(float) * elements_per_proc);
    float *sub_C = (float *)malloc(sizeof(float) * elements_per_proc);

    // --- CHANGE 3: Rank 0 Manually scans the vectors ---
    if (world_rank == 0) {
        A = (float *)malloc(sizeof(float) * total_elements);
        B = (float *)malloc(sizeof(float) * total_elements);
        C = (float *)malloc(sizeof(float) * total_elements);

        printf("Enter elements for Array A (x y z format for each vector):\n");
        for (int i = 0; i < N; i++) {
            printf("Vector A[%d]: ", i);
            fflush(stdout);
            scanf("%f %f %f", &A[i*3], &A[i*3+1], &A[i*3+2]);
        }

        printf("Enter elements for Array B (x y z format for each vector):\n");
        for (int i = 0; i < N; i++) {
            printf("Vector B[%d]: ", i);
            fflush(stdout);
            scanf("%f %f %f", &B[i*3], &B[i*3+1], &B[i*3+2]);
        }
    }

    // The rest of the parallel logic remains exactly the same
    MPI_Scatter(A, elements_per_proc, MPI_FLOAT, sub_A, elements_per_proc, MPI_FLOAT, 0, MPI_COMM_WORLD);
    MPI_Scatter(B, elements_per_proc, MPI_FLOAT, sub_B, elements_per_proc, MPI_FLOAT, 0, MPI_COMM_WORLD);

    for (int i = 0; i < elements_per_proc; i += 3) {
        float ax = sub_A[i];   float ay = sub_A[i+1]; float az = sub_A[i+2];
        float bx = sub_B[i];   float by = sub_B[i+1]; float bz = sub_B[i+2];

        sub_C[i]   = (ay * bz) - (az * by);
        sub_C[i+1] = (az * bx) - (ax * bz);
        sub_C[i+2] = (ax * by) - (ay * bx);
    }

    MPI_Gather(sub_C, elements_per_proc, MPI_FLOAT, C, elements_per_proc, MPI_FLOAT, 0, MPI_COMM_WORLD);

    // --- CHANGE 4: Print ALL results ---
    if (world_rank == 0) {
        printf("\n--- Results ---\n");
        for (int i = 0; i < N; i++) {
            printf("Vector %d: <%.1f, %.1f, %.1f>\n", i, C[i*3], C[i*3+1], C[i*3+2]);
        }
        free(A); free(B); free(C);
    }

    free(sub_A); free(sub_B); free(sub_C);
    MPI_Finalize();
    return 0;
}
