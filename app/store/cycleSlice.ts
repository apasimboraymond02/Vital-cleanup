import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { Cycle } from '../types';

interface CycleState {
  cycles: Cycle[];
  currentCycle: Cycle | null;
  loading: boolean;
  error: string | null;
}

const initialState: CycleState = {
  cycles: [],
  currentCycle: null,
  loading: false,
  error: null,
};

export const fetchCycles = createAsyncThunk(
  'cycles/fetchCycles',
  async (_, { rejectWithValue }) => {
    try {
      // This would fetch from database in production
      return [];
    } catch (error) {
      return rejectWithValue('Failed to fetch cycles');
    }
  }
);

export const addCycle = createAsyncThunk(
  'cycles/addCycle',
  async (cycle: Omit<Cycle, 'id' | 'createdAt' | 'updatedAt'>, { rejectWithValue }) => {
    try {
      // This would save to database in production
      const newCycle: Cycle = {
        ...cycle,
        id: Date.now().toString(),
        createdAt: new Date(),
        updatedAt: new Date(),
      };
      return newCycle;
    } catch (error) {
      return rejectWithValue('Failed to add cycle');
    }
  }
);

const cycleSlice = createSlice({
  name: 'cycles',
  initialState,
  reducers: {
    setCurrentCycle: (state, action) => {
      state.currentCycle = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCycles.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchCycles.fulfilled, (state, action) => {
        state.loading = false;
        state.cycles = action.payload;
      })
      .addCase(fetchCycles.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(addCycle.fulfilled, (state, action) => {
        state.cycles.unshift(action.payload);
        state.currentCycle = action.payload;
      })
      .addCase(addCycle.rejected, (state, action) => {
        state.error = action.payload as string;
      });
  },
});

export const { setCurrentCycle, clearError } = cycleSlice.actions;
export default cycleSlice.reducer;