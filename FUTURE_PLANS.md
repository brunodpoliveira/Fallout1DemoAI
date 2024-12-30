# Fallout1DemoAI Development Roadmap

## Current Implementation Status

### ✅ Completed Features
- Core Agent interface & types (Agent, AgentInput, AgentOutput, Decision)
- Basic BaseNPC implementation with state management
- AgentManager for managing agent instances
- Player integration through PlayerAgent class
- Basic agent decision framework
- Dialog system integration with agent decisions
- Basic faction-based interaction rules
- Basic task system groundwork
- Initial NPCManager integration with AgentManager

### 🚧 Partially Implemented Features
1. Interaction System
    - Basic agent interaction flow
    - Dialog system with agent decisions
    - Basic state tracking (Idle, Busy, InConversation)

2. Game Loop Integration
    - Basic decision-making
    - Initial state management

## Implementation Order

### Phase 1: Memory System Foundation (2-3 weeks)
1. Memory Data Structures
   ```kotlin
   data class Memory(
       val id: String,
       val description: String,
       val importance: Float,
       val lastAccess: Long,
       val embedding: List<Float>,
       val type: MemoryType,
       val data: MemoryData
   )
   ```

2. Core Components
    - Memory Manager implementation
    - Vector embeddings integration
    - Importance calculation system
    - Basic reflection system
    - Memory storage/retrieval
    - Memory ranking system

3. Memory Types
    - Conversation memories
    - Observation memories
    - Interaction memories
    - Reflection memories

### Phase 2: Enhanced Action Model (2 weeks)
1. Action System
   ```kotlin
   sealed class NPCAction {
       data class InitiateConversation(val targetNPC: String) : NPCAction()
       data class JoinActivity(val activityId: String) : NPCAction()
       data class ShareInformation(val targetNPC: String, val memoryId: String) : NPCAction()
       data class FormRelationship(val targetNPC: String, val type: RelationType) : NPCAction()
       data class ExploreArea(val location: Point) : NPCAction()
   }
   ```

2. Implementation Steps
    - NPC-to-NPC action framework
    - Basic planning system
    - Activity scheduling
    - Conversation initiation logic
    - Action consequences system

### Phase 3: Environmental Awareness (2 weeks)
1. Core Systems
    - Area detection
    - Event awareness
    - Location memory/familiarity
    - Activity zones
    - Environmental state tracking

2. Integration Components
    - Spatial awareness system
    - Event broadcasting
    - Zone management
    - State persistence

### Phase 4: Advanced NPC Behaviors (2-3 weeks)
1. Autonomous Systems
    - Decision-making cycle
    - Activity scheduling
    - Resource management
    - Inter-NPC interactions

2. Behavior Components
    - Relationship system
    - Dynamic scheduling
    - Need-based decisions
    - Group behavior logic

### Phase 5: Integration & Polish (2 weeks)
1. System Integration
    - Memory-Action integration
    - Environmental awareness hooks
    - Complete state management
    - Full event system

2. Performance Optimization
    - Memory batch updates
    - Pathfinding optimization
    - State caching
    - Spatial partitioning

## Testing Strategy

### Unit Tests
- Memory system components
- Action execution logic
- State transitions
- Decision-making paths

### Integration Tests
- NPC autonomous movement
- Inter-NPC interactions
- Memory-action feedback loops
- Environmental response system

### System Tests
- Full agent system validation
- Memory persistence
- Performance benchmarks
- Stress testing

## Success Metrics
NPCs should demonstrate:
- Memory persistence and recall
- Dynamic relationship formation
- Contextual conversation ability
- Activity participation
- Environmental adaptation
- Context-aware decision making
- Resource management
- Social interaction capabilities

## Additional Resources

### Tools
- 3D Model Generation: https://huggingface.co/spaces/JeffreyXiang/TRELLIS
- Auto-Rigging: https://actorcore.reallusion.com/auto-rig

### Performance Considerations
- Memory optimization
- State synchronization
- Concurrent action handling
- Spatial query optimization
- Break the level into smaller chunks/tiles and only apply fog effects to the chunks near the player

## Documentation Requirements
- Architecture documentation
- API documentation
- Integration guides
- Performance guidelines
- Testing procedures


