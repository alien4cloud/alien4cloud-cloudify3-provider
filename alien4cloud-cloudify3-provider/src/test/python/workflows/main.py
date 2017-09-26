from graph import Graph
from graph import print_graph
from context import build_context
from tasks import build_tasks

graph = Graph()
ctx = build_context()
custom_context = build_tasks(ctx, graph)
print_graph(graph)
