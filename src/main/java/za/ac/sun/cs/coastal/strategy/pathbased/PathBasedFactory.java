package za.ac.sun.cs.coastal.strategy.pathbased;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import za.ac.sun.cs.coastal.COASTAL;
import za.ac.sun.cs.coastal.Trigger;
import za.ac.sun.cs.coastal.diver.SymbolicState;
import za.ac.sun.cs.coastal.messages.Broker;
import za.ac.sun.cs.coastal.messages.Tuple;
import za.ac.sun.cs.coastal.pathtree.PathTree;
import za.ac.sun.cs.coastal.pathtree.PathTreeNode;
import za.ac.sun.cs.coastal.solver.Expression;
import za.ac.sun.cs.coastal.solver.Solver;
import za.ac.sun.cs.coastal.strategy.StrategyFactory;
import za.ac.sun.cs.coastal.symbolic.Execution;
import za.ac.sun.cs.coastal.symbolic.Input;
import za.ac.sun.cs.coastal.symbolic.Path;

public abstract class PathBasedFactory implements StrategyFactory {

    // ======================================================================
    //
    // PATH-BASED SEARCH STRATEGY MANAGER
    //
    // ======================================================================

    public abstract static class PathBasedManager implements StrategyManager {

        protected final COASTAL coastal;

        protected final Broker broker;

        protected final PathTree pathTree;

        /**
         * Counter of number of refinements.
         */
        protected final AtomicLong refineCount = new AtomicLong(0);

        /**
         * Accumulator of all the solver times.
         */
        protected final AtomicLong solverTime = new AtomicLong(0);

        /**
         * Accumulator of all the model extraction times.
         */
        protected final AtomicLong extractionTime = new AtomicLong(0);

        /**
         * Accumulator of all the refinement times.
         */
        protected final AtomicLong strategyTime = new AtomicLong(0);

        /**
         * Accumulator of all the strategy waiting times.
         */
        private final AtomicLong strategyWaitTime = new AtomicLong(0);

        /**
         * Counter for the strategy waiting times.
         */
        private final AtomicLong strategyWaitCount = new AtomicLong(0);

        public PathBasedManager(COASTAL coastal) {
            this.coastal = coastal;
            broker = coastal.getBroker();
            broker.subscribe("coastal-stop", this::report);
            pathTree = coastal.getPathTree();
        }

        public PathTree getPathTree() {
            return pathTree;
        }

        public PathTreeNode insertPath0(Execution execution, boolean infeasible) {
            return pathTree.insertPath(execution, infeasible);
        }

        public boolean insertPath(Execution execution, boolean infeasible) {
            return (pathTree.insertPath(execution, infeasible) == null);
        }

        public boolean insertPath(Path path, boolean infeasible) {
            return (pathTree.insertPath(new Execution(path, null), infeasible) == null);
        }

        /**
         * Increment the number of refinements.
         */
        public void incrementRefinements() {
            refineCount.incrementAndGet();
        }

        public void recordSolverTime(long time) {
            solverTime.addAndGet(time);
        }

        public void recordExtractionTime(long time) {
            extractionTime.addAndGet(time);
        }

        /**
         * Add a reported dive time to the accumulator that tracks how long the dives
         * took.
         *
         * @param time the time for this dive
         */
        public void recordTime(long time) {
            strategyTime.addAndGet(time);
        }

        /**
         * Add a reported strategy wait time. This is used to determine if it makes
         * sense to create additional threads (or destroy them).
         *
         * @param time the wait time for this strategy
         */
        public void recordWaitTime(long time) {
            strategyWaitTime.addAndGet(time);
            strategyWaitCount.incrementAndGet();
        }

        public void report(Object object) {
            String name = getName();
            double swt = strategyWaitTime.get() / strategyWaitCount.doubleValue();
            broker.publish("report", new Tuple(name + ".tasks", getTaskCount()));
            broker.publish("report", new Tuple(name + ".solver-time", solverTime.get()));
            broker.publish("report", new Tuple(name + ".extraction-time", extractionTime.get()));
            broker.publish("report", new Tuple(name + ".wait-time", swt));
            broker.publish("report", new Tuple(name + ".total-time", strategyTime.get()));
        }

        protected abstract int getTaskCount();

        private static final String[] PROPERTY_NAMES = new String[]{"#tasks", "#refinements", "waiting time",
                "total time"};

        @Override
        public String[] getPropertyNames() {
            return PROPERTY_NAMES;
        }

        @Override
        public Object[] getPropertyValues() {
            Object[] propertyValues = new Object[4];
            int index = 0;
            double swt = strategyWaitTime.get() / strategyWaitCount.doubleValue();
            long c = refineCount.get();
            long t = strategyTime.get();
            propertyValues[index++] = getTaskCount();
            propertyValues[index++] = String.format("%d (%.1f/sec)", c, c / (0.001 * t));
            propertyValues[index++] = swt;
            propertyValues[index++] = t;
            return propertyValues;
        }

    }

    // ======================================================================
    //
    // PATH-BASED SEARCH STRATEGY
    //
    // ======================================================================

    public abstract static class PathBasedStrategy extends Strategy {

        protected final PathBasedManager manager;

        protected final Broker broker;

        protected final Solver solver;

        protected final Set<String> visitedInputs = new HashSet<>();

        public PathBasedStrategy(COASTAL coastal, StrategyManager manager) {
            super(coastal, manager);
            this.manager = (PathBasedManager) manager;
            broker = coastal.getBroker();
            solver = Solver.getSolver(coastal);
        }

        private Input getModelFromString(List<Integer> inputString) {
            Input resultInput = new Input();
            Trigger trigger = coastal.getTrigger(0);
            String name = trigger.getParamName(0);
            for (int i = 0; i < inputString.size(); i++) {
                resultInput.put(name + SymbolicState.INDEX_SEPARATOR + i, new Long(inputString.get(i)));
            }
            return resultInput;
        }

        @Override
        public Void call() throws Exception {
            log.trace("^^^ strategy task starting");
            try {
                while (true) {
                    long t0 = System.currentTimeMillis();
                    Execution execution = coastal.getNextPc();
                    long t1 = System.currentTimeMillis();
                    manager.recordWaitTime(t1 - t0);
//					manager.incrementRefinements();
//					log.trace("+++ starting refinement");
//					List<Input> inputs = refine(execution);

                    // Check Redis for work

                    List<Integer> values = new LinkedList<>();
                    values.add(12);
                    values.add(12);

                    List<Input> inputs = Collections.singletonList(
                            getModelFromString(values));

                    // We start at -1 (and not at 0) to account for the
                    // fact that one item of work has been removed from
                    // the global work count.
                    int d = -1;
                    int m = coastal.addDiverInputs(inputs);
                    log.info("Added new input: " + inputs.get(0));
                    d += m;
                    coastal.updateWork(d);
                }
            } catch (InterruptedException e) {
                log.trace("^^^ strategy task canceled");
                throw e;
            }
        }

        protected List<Input> refine(Execution execution) {
            long t0 = System.currentTimeMillis();
            List<Input> newModels = refine0(execution);
            manager.recordTime(System.currentTimeMillis() - t0);
            return newModels;
        }

        protected List<Input> refine0(Execution execution) {
            if (execution == null) {
                return null;
            }
            Path path = execution.getPath();
            if (path == null) {
                log.trace("... explored <> EMPTY PATH");
                manager.insertPath(execution, false); // ignore revisited return value
                return null;
            } else {
                log.trace("... explored <{}> {}", path.getSignature(), path.getPathCondition().toString());
                manager.insertPath(execution, false); // ignore revisited return value
                return refine1();
            }
        }

        protected List<Input> refine1() {
            while (true) {
                Path path = findNewPath(manager.getPathTree());
                if (path == null) {
                    log.trace("... no further paths");
                    return null;
                    // log.trace("...Tree shape: {}", pathTree.getShape());
                }
                Expression pc = path.getPathCondition();
                String sig = path.getSignature();
                log.trace("... trying   <{}> {}", sig, pc.toString());
                long t = System.currentTimeMillis();
                Input input = solver.solve(pc);
                manager.recordSolverTime(System.currentTimeMillis() - t);
                if (input == null) {
                    log.trace("... no model");
                    log.trace("(The spc is {})", path.getPathCondition().toString());
                    manager.insertPath(path, true);
                } else {
                    String inputString = input.toMapString();
                    log.trace("... new model: {}", inputString);
                    if (visitedInputs.add(inputString)) {
                        return Collections.singletonList(input);
                    } else {
                        manager.insertPath(path, false);
                        log.trace("... model {} has been visited before, retrying", inputString);
                    }
                }
            }
        }

        protected abstract Path findNewPath(PathTree pathTree);

    }

}
